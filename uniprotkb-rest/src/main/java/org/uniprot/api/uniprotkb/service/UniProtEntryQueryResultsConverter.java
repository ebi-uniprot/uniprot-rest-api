package org.uniprot.api.uniprotkb.service;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.uniprotkb.controller.request.FieldsParser;
import org.uniprot.api.uniprotkb.repository.store.UniProtKBStoreClient;
import org.uniprot.core.impl.SequenceBuilder;
import org.uniprot.core.json.parser.uniprot.UniprotkbJsonConfig;
import org.uniprot.core.taxonomy.TaxonomyEntry;
import org.uniprot.core.uniprotkb.*;
import org.uniprot.core.uniprotkb.impl.EntryInactiveReasonBuilder;
import org.uniprot.core.uniprotkb.impl.UniProtkbAccessionBuilder;
import org.uniprot.core.uniprotkb.impl.UniProtkbEntryBuilder;
import org.uniprot.core.uniprotkb.impl.UniProtkbIdBuilder;
import org.uniprot.store.search.document.uniprot.UniProtDocument;
import org.uniprot.store.search.field.UniProtField;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The purpose of this class is to simplify conversion of {@code QueryResult<UniProtDocument>}
 * instances to {@code QueryResult<UniProtkbEntry>} or {@code Optional<UniProtkbEntry>}. It is used
 * in {@link UniProtEntryService}.
 *
 * <p>Note, this class will be replaced once we finalise a core domain model, which is the single
 * entity returned from by requests -- and on which all message converters will operate.
 *
 * <p>Created 18/10/18
 *
 * @author Edd
 */
class UniProtEntryQueryResultsConverter {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(UniProtEntryQueryResultsConverter.class);

    private final UniProtKBStoreClient entryStore;
    private final RetryPolicy<Object> retryPolicy =
            new RetryPolicy<>()
                    .handle(IOException.class)
                    .withDelay(Duration.ofMillis(100))
                    //      .withDelay(500,TimeUnit.MILLISECONDS)
                    .withMaxRetries(5);
    private final TaxonomyService taxonomyService;

    UniProtEntryQueryResultsConverter(
            UniProtKBStoreClient entryStore, TaxonomyService taxonomyService) {
        this.entryStore = entryStore;
        this.taxonomyService = taxonomyService;
    }

    QueryResult<UniProtkbEntry> convertQueryResult(
            QueryResult<UniProtDocument> results, Map<String, List<String>> filters) {
        List<UniProtkbEntry> upEntries =
                results.getContent().stream()
                        .map(doc -> convertDoc(doc, filters))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toList());
        return QueryResult.of(
                upEntries, results.getPage(), results.getFacets(), results.getMatchedFields());
    }

    Optional<UniProtkbEntry> convertDoc(UniProtDocument doc, Map<String, List<String>> filters) {
        if (doc.active) {
            Optional<UniProtkbEntry> opEntry = getEntryFromStore(doc, filters);
            if (hasLineage(filters)) {
                return addLineage(opEntry);
            } else return opEntry;
        } else {
            return getInactiveUniProtEntry(doc);
        }
    }

    private boolean hasLineage(Map<String, List<String>> filters) {
        return filters.containsKey("lineage");
    }

    private Optional<UniProtkbEntry> addLineage(Optional<UniProtkbEntry> opEntry) {
        if (opEntry.isPresent()) {
            TaxonomyEntry taxEntry =
                    taxonomyService.findById(opEntry.get().getOrganism().getTaxonId());
            if (taxEntry == null) {
                return opEntry;
            }
            UniProtkbEntryBuilder builder = UniProtkbEntryBuilder.from(opEntry.get());
            return Optional.of(builder.lineagesSet(taxEntry.getLineages()).build());
        } else return opEntry;
    }

    private Optional<UniProtkbEntry> getInactiveUniProtEntry(UniProtDocument doc) {
        UniProtkbAccession accession = new UniProtkbAccessionBuilder(doc.accession).build();
        List<String> mergeDemergeList = new ArrayList<>();

        String[] reasonItems = doc.inactiveReason.split(":");
        InactiveReasonType type = InactiveReasonType.valueOf(reasonItems[0].toUpperCase());
        if (reasonItems.length > 1) {
            mergeDemergeList.addAll(Arrays.asList(reasonItems[1].split(",")));
        }

        UniProtkbId uniProtkbId = new UniProtkbIdBuilder(doc.id).build();
        EntryInactiveReason inactiveReason =
                new EntryInactiveReasonBuilder()
                        .type(type)
                        .mergeDemergeTosSet(mergeDemergeList)
                        .build();

        UniProtkbEntryBuilder entryBuilder =
                new UniProtkbEntryBuilder(accession, uniProtkbId, inactiveReason);
        return Optional.of(entryBuilder.build());
    }

    private Optional<UniProtkbEntry> getEntryFromStore(
            UniProtDocument doc, Map<String, List<String>> filters) {
        if (FieldsParser.isDefaultFilters(filters) && (doc.avroBinary != null)) {
            UniProtkbEntry uniProtkbEntry = null;

            try {
                byte[] decodeEntry = Base64.getDecoder().decode(doc.avroBinary);
                ObjectMapper jsonMapper = UniprotkbJsonConfig.getInstance().getFullObjectMapper();
                uniProtkbEntry = jsonMapper.readValue(decodeEntry, UniProtkbEntry.class);
            } catch (IOException e) {
                LOGGER.info("Error converting solr avro_binary default UniProtkbEntry", e);
            }
            if (Objects.isNull(uniProtkbEntry)) {
                return Optional.empty();
            }
            if (filters.containsKey(UniProtField.ResultFields.mass.name())
                    || filters.containsKey(UniProtField.ResultFields.length.name())) {
                char[] fakeSeqArrayWithCorrectLength = new char[doc.seqLength];
                Arrays.fill(fakeSeqArrayWithCorrectLength, 'X');
                SequenceBuilder seq =
                        new SequenceBuilder(new String(fakeSeqArrayWithCorrectLength));
                // seq.molWeight(doc.seqMass); //TODO: TRM-22339 assigned to Jie
                UniProtkbEntryBuilder entryBuilder = UniProtkbEntryBuilder.from(uniProtkbEntry);
                entryBuilder.sequence(seq.build());
                uniProtkbEntry = entryBuilder.build();
            }
            return Optional.of(uniProtkbEntry);
        } else {
            return Failsafe.with(retryPolicy).get(() -> entryStore.getEntry(doc.accession));
        }
    }
}
