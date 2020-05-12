package org.uniprot.api.uniprotkb.service;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.uniprotkb.repository.store.UniProtKBStoreClient;
import org.uniprot.core.taxonomy.TaxonomyEntry;
import org.uniprot.core.uniprotkb.*;
import org.uniprot.core.uniprotkb.impl.EntryInactiveReasonBuilder;
import org.uniprot.core.uniprotkb.impl.UniProtKBAccessionBuilder;
import org.uniprot.core.uniprotkb.impl.UniProtKBEntryBuilder;
import org.uniprot.core.uniprotkb.impl.UniProtKBIdBuilder;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.returnfield.model.ReturnField;
import org.uniprot.store.search.document.uniprot.UniProtDocument;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The purpose of this class is to simplify conversion of {@code QueryResult<UniProtDocument>}
 * instances to {@code QueryResult<UniProtKBEntry>} or {@code Optional<UniProtKBEntry>}. It is used
 * in {@link UniProtEntryService}.
 *
 * <p>Note, this class will be replaced once we finalise a core domain model, which is the single
 * entity returned from by requests -- and on which all message converters will operate.
 *
 * <p>Created 18/10/18
 *
 * @author Edd
 */
@Service
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

    QueryResult<UniProtKBEntry> convertQueryResult(
            QueryResult<UniProtDocument> results, List<ReturnField> filters) {
        List<UniProtKBEntry> upEntries =
                results.getContent().stream()
                        .map(doc -> convertDoc(doc, filters))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toList());
        return QueryResult.of(
                upEntries, results.getPage(), results.getFacets(), results.getMatchedFields());
    }

    Optional<UniProtKBEntry> convertDoc(UniProtDocument doc, List<ReturnField> filters) {
        if (doc.active) {
            Optional<UniProtKBEntry> opEntry = getEntryFromStore(doc);
            if (hasLineage(filters) && opEntry.isPresent()) {
                return addLineage(opEntry.get());
            } else return opEntry;
        } else {
            return getInactiveUniProtEntry(doc);
        }
    }

    private boolean hasLineage(List<ReturnField> filters) {
        return filters.stream().map(ReturnField::getName).anyMatch("lineage"::equals);
    }

    private Optional<UniProtKBEntry> addLineage(UniProtKBEntry entry) {
        TaxonomyEntry taxEntry = taxonomyService.findById(entry.getOrganism().getTaxonId());
        if (Utils.notNull(taxEntry)) {
            UniProtKBEntryBuilder builder = UniProtKBEntryBuilder.from(entry);
            return Optional.of(builder.lineagesSet(taxEntry.getLineages()).build());
        } else {
            return Optional.of(entry);
        }
    }

    private Optional<UniProtKBEntry> getInactiveUniProtEntry(UniProtDocument doc) {
        UniProtKBAccession accession = new UniProtKBAccessionBuilder(doc.accession).build();
        List<String> mergeDemergeList = new ArrayList<>();

        String[] reasonItems = doc.inactiveReason.split(":");
        InactiveReasonType type = InactiveReasonType.valueOf(reasonItems[0].toUpperCase());
        if (reasonItems.length > 1) {
            mergeDemergeList.addAll(Arrays.asList(reasonItems[1].split(",")));
        }

        UniProtKBId uniProtkbId = new UniProtKBIdBuilder(doc.id).build();
        EntryInactiveReason inactiveReason =
                new EntryInactiveReasonBuilder()
                        .type(type)
                        .mergeDemergeTosSet(mergeDemergeList)
                        .build();

        UniProtKBEntryBuilder entryBuilder =
                new UniProtKBEntryBuilder(accession, uniProtkbId, inactiveReason);
        return Optional.of(entryBuilder.build());
    }

    private Optional<UniProtKBEntry> getEntryFromStore(UniProtDocument doc) {
        return Failsafe.with(retryPolicy).get(() -> entryStore.getEntry(doc.accession));
    }
}
