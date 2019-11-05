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
import org.uniprot.core.builder.SequenceBuilder;
import org.uniprot.core.json.parser.uniprot.UniprotJsonConfig;
import org.uniprot.core.taxonomy.TaxonomyEntry;
import org.uniprot.core.uniprot.*;
import org.uniprot.core.uniprot.builder.EntryInactiveReasonBuilder;
import org.uniprot.core.uniprot.builder.UniProtAccessionBuilder;
import org.uniprot.core.uniprot.builder.UniProtEntryBuilder;
import org.uniprot.core.uniprot.builder.UniProtIdBuilder;
import org.uniprot.store.search.document.uniprot.UniProtDocument;
import org.uniprot.store.search.field.UniProtField;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The purpose of this class is to simplify conversion of {@code QueryResult<UniProtDocument>}
 * instances to {@code QueryResult<UniProtEntry>} or {@code Optional<UniProtEntry>}. It is used in
 * {@link UniProtEntryService}.
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

    QueryResult<UniProtEntry> convertQueryResult(
            QueryResult<UniProtDocument> results, Map<String, List<String>> filters) {
        List<UniProtEntry> upEntries =
                results.getContent().stream()
                        .map(doc -> convertDoc(doc, filters))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toList());
        return QueryResult.of(
                upEntries, results.getPage(), results.getFacets(), results.getMatchedFields());
    }

    Optional<UniProtEntry> convertDoc(UniProtDocument doc, Map<String, List<String>> filters) {
        if (doc.active) {
            Optional<UniProtEntry> opEntry = getEntryFromStore(doc, filters);
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

    private Optional<UniProtEntry> addLineage(Optional<UniProtEntry> opEntry) {
        if (opEntry.isPresent()) {
            TaxonomyEntry taxEntry =
                    taxonomyService.findById(opEntry.get().getOrganism().getTaxonId());
            if (taxEntry == null) {
                return opEntry;
            }
            UniProtEntryBuilder.ActiveEntryBuilder builder =
                    new UniProtEntryBuilder().from(opEntry.get());
            return Optional.of(builder.lineages(taxEntry.getLineage()).build());
        } else return opEntry;
    }

    private Optional<UniProtEntry> getInactiveUniProtEntry(UniProtDocument doc) {
        UniProtAccession accession = new UniProtAccessionBuilder(doc.accession).build();
        List<String> mergeDemergeList = new ArrayList<>();

        String[] reasonItems = doc.inactiveReason.split(":");
        InactiveReasonType type = InactiveReasonType.valueOf(reasonItems[0].toUpperCase());
        if (reasonItems.length > 1) {
            mergeDemergeList.addAll(Arrays.asList(reasonItems[1].split(",")));
        }

        UniProtId uniProtId = new UniProtIdBuilder(doc.id).build();
        EntryInactiveReason inactiveReason =
                new EntryInactiveReasonBuilder()
                        .type(type)
                        .mergeDemergeTo(mergeDemergeList)
                        .build();

        UniProtEntryBuilder.InactiveEntryBuilder entryBuilder =
                new UniProtEntryBuilder()
                        .primaryAccession(accession)
                        .uniProtId(uniProtId)
                        .inactive()
                        .inactiveReason(inactiveReason);
        return Optional.of(entryBuilder.build());
    }

    private Optional<UniProtEntry> getEntryFromStore(
            UniProtDocument doc, Map<String, List<String>> filters) {
        if (FieldsParser.isDefaultFilters(filters) && (doc.avroBinary != null)) {
            UniProtEntry uniProtEntry = null;

            try {
                byte[] decodeEntry = Base64.getDecoder().decode(doc.avroBinary);
                ObjectMapper jsonMapper = UniprotJsonConfig.getInstance().getFullObjectMapper();
                uniProtEntry = jsonMapper.readValue(decodeEntry, UniProtEntry.class);
            } catch (IOException e) {
                LOGGER.info("Error converting solr avro_binary default UniProtEntry", e);
            }
            if (Objects.isNull(uniProtEntry)) {
                return Optional.empty();
            }
            if (filters.containsKey(UniProtField.ResultFields.mass.name())
                    || filters.containsKey(UniProtField.ResultFields.length.name())) {
                char[] fakeSeqArrayWithCorrectLength = new char[doc.seqLength];
                Arrays.fill(fakeSeqArrayWithCorrectLength, 'X');
                SequenceBuilder seq =
                        new SequenceBuilder(new String(fakeSeqArrayWithCorrectLength));
                // seq.molWeight(doc.seqMass); //TODO: TRM-22339 assigned to Jie
                UniProtEntryBuilder.ActiveEntryBuilder entryBuilder =
                        new UniProtEntryBuilder().from(uniProtEntry);
                entryBuilder.sequence(seq.build());
                uniProtEntry = entryBuilder.build();
            }
            return Optional.of(uniProtEntry);
        } else {
            return Failsafe.with(retryPolicy).get(() -> entryStore.getEntry(doc.accession));
        }
    }
}
