package uk.ac.ebi.uniprot.api.uniprotkb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.uniprot.api.common.repository.search.QueryResult;
import uk.ac.ebi.uniprot.api.uniprotkb.controller.request.FieldsParser;
import uk.ac.ebi.uniprot.api.uniprotkb.repository.store.UniProtStoreClient;
import uk.ac.ebi.uniprot.api.uniprotkb.service.filters.FilterComponentType;
import uk.ac.ebi.uniprot.domain.builder.SequenceBuilder;
import uk.ac.ebi.uniprot.domain.uniprot.*;
import uk.ac.ebi.uniprot.domain.uniprot.builder.EntryInactiveReasonBuilder;
import uk.ac.ebi.uniprot.domain.uniprot.builder.UniProtAccessionBuilder;
import uk.ac.ebi.uniprot.domain.uniprot.builder.UniProtEntryBuilder;
import uk.ac.ebi.uniprot.domain.uniprot.builder.UniProtIdBuilder;
import uk.ac.ebi.uniprot.json.parser.uniprot.UniprotJsonConfig;
import uk.ac.ebi.uniprot.search.document.uniprot.UniProtDocument;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * The purpose of this class is to simplify conversion of {@code QueryResult<UniProtDocument>} instances to
 * {@code QueryResult<UniProtEntry>} or {@code Optional<UniProtEntry>}. It is used in {@link UniProtEntryService}.
 *
 * Note, this class will be replaced once we finalise a core domain model, which is the single entity returned
 * from by requests -- and on which all message converters will operate.
 *
 * Created 18/10/18
 *
 * @author Edd
 */
class UniProtEntryQueryResultsConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(UniProtEntryQueryResultsConverter.class);

    private final UniProtStoreClient entryStore;
    private final RetryPolicy retryPolicy = new RetryPolicy()
            .retryOn(IOException.class)
            .withDelay(500,TimeUnit.MILLISECONDS)
            .withMaxRetries(5);

    UniProtEntryQueryResultsConverter(UniProtStoreClient entryStore) {
        this.entryStore = entryStore;
    }

    QueryResult<UniProtEntry> convertQueryResult(QueryResult<UniProtDocument> results, Map<String, List<String>> filters) {
        List<UniProtEntry> upEntries = results.getContent()
                .stream().map(doc -> convertDoc(doc, filters))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
        return QueryResult.of(upEntries, results.getPage(), results.getFacets());
    }

    Optional<UniProtEntry> convertDoc(UniProtDocument doc, Map<String, List<String>> filters) {
        if (doc.active) {
            return getEntryFromStore(doc, filters);
        } else {
            return getInactiveUniProtEntry(doc);
        }
    }

    private Optional<UniProtEntry> getInactiveUniProtEntry(UniProtDocument doc) {
        UniProtAccession accession = new UniProtAccessionBuilder(doc.accession).build();
        List<String> mergeDemergeList = new ArrayList<>();

        String[] reasonItems =  doc.inactiveReason.split(":");
        InactiveReasonType type = InactiveReasonType.valueOf(reasonItems[0].toUpperCase());
        if(reasonItems.length > 1){
            mergeDemergeList.addAll(Arrays.asList(reasonItems[1].split(",")));
        }

        UniProtId uniProtId = new UniProtIdBuilder(doc.id).build();
        EntryInactiveReason inactiveReason = new EntryInactiveReasonBuilder()
                .type(type)
                .mergeDemergeTo(mergeDemergeList)
                .build();

        UniProtEntryBuilder.InactiveEntryBuilder entryBuilder = new UniProtEntryBuilder()
                .primaryAccession(accession)
                .uniProtId(uniProtId)
                .inactive()
                .inactiveReason(inactiveReason);
        return Optional.of(entryBuilder.build());
    }

    private Optional<UniProtEntry> getEntryFromStore(UniProtDocument doc, Map<String, List<String>> filters) {
        if (FieldsParser.isDefaultFilters(filters) && (doc.avro_binary != null)) {
            UniProtEntry uniProtEntry = null;

            try {
                byte[] decodeEntry = Base64.getDecoder().decode(doc.avro_binary);
                ObjectMapper jsonMapper = UniprotJsonConfig.getInstance().getFullObjectMapper();
                uniProtEntry = jsonMapper.readValue(decodeEntry, UniProtEntry.class);
            }catch (IOException e){
                LOGGER.info("Error converting solr avro_binary default UniProtEntry",e);
            }
            if (Objects.isNull(uniProtEntry)) {
                return Optional.empty();
            }
            if (filters.containsKey(FilterComponentType.MASS.name().toLowerCase())
                    || filters.containsKey(FilterComponentType.LENGTH.name().toLowerCase())) {
                char[] fakeSeqArrayWithCorrectLength = new char[doc.seqLength];
                Arrays.fill(fakeSeqArrayWithCorrectLength, 'X');
                SequenceBuilder seq = new SequenceBuilder(new String(fakeSeqArrayWithCorrectLength));

                UniProtEntryBuilder.ActiveEntryBuilder entryBuilder = new UniProtEntryBuilder().from(uniProtEntry);
                entryBuilder.sequence(seq.build());
                uniProtEntry = entryBuilder.build();
            }
            return Optional.of(uniProtEntry);
        } else {
            return Failsafe.with(retryPolicy).get(() -> entryStore.getEntry(doc.accession));                     
        }
    }
}
