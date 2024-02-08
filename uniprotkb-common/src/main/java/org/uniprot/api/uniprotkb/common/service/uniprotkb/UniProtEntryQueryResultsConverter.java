package org.uniprot.api.uniprotkb.common.service.uniprotkb;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

import org.springframework.stereotype.Service;
import org.uniprot.api.common.exception.ServiceException;
import org.uniprot.api.common.repository.search.ProblemPair;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.stream.store.uniprotkb.TaxonomyLineageService;
import org.uniprot.api.uniprotkb.common.repository.store.UniProtKBStoreClient;
import org.uniprot.core.taxonomy.TaxonomyEntry;
import org.uniprot.core.uniprotkb.*;
import org.uniprot.core.uniprotkb.impl.EntryInactiveReasonBuilder;
import org.uniprot.core.uniprotkb.impl.UniProtKBAccessionBuilder;
import org.uniprot.core.uniprotkb.impl.UniProtKBEntryBuilder;
import org.uniprot.core.uniprotkb.impl.UniProtKBIdBuilder;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.returnfield.model.ReturnField;
import org.uniprot.store.search.document.uniprot.UniProtDocument;

/**
 * The purpose of this class is to simplify conversion of {@code QueryResult<UniProtDocument>}
 * instances to {@code QueryResult<UniProtKBEntry>} or {@code Optional<UniProtKBEntry>}. It is used
 * in {@link org.uniprot.api.uniprotkb.common.service.uniprotkb.UniProtEntryService}.
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
    private final UniProtKBStoreClient entryStore;
    private final RetryPolicy<Object> retryPolicy =
            new RetryPolicy<>()
                    .handle(IOException.class)
                    .withDelay(Duration.ofMillis(100))
                    .withMaxRetries(5);
    private final TaxonomyLineageService taxonomyLineageService;

    UniProtEntryQueryResultsConverter(
            UniProtKBStoreClient entryStore, TaxonomyLineageService taxonomyLineageService) {
        this.entryStore = entryStore;
        this.taxonomyLineageService = taxonomyLineageService;
    }

    QueryResult<UniProtKBEntry> convertQueryResult(
            QueryResult<UniProtDocument> results,
            List<ReturnField> filters,
            Set<ProblemPair> warnings) {
        Stream<UniProtKBEntry> upEntries =
                results.getContent()
                        .map(doc -> convertDoc(doc, filters))
                        .filter(Optional::isPresent)
                        .map(Optional::get);
        return QueryResult.<UniProtKBEntry>builder()
                .content(upEntries)
                .page(results.getPage())
                .facets(results.getFacets())
                .matchedFields(results.getMatchedFields())
                .suggestions(results.getSuggestions())
                .warnings(warnings)
                .build();
    }

    Optional<UniProtKBEntry> convertDoc(UniProtDocument doc, List<ReturnField> filters) {
        if (doc.active) {
            Optional<UniProtKBEntry> opEntry = getEntryFromStore(doc);
            return opEntry.map(entry -> addLineageIfRequested(filters, entry))
                    .orElseThrow(
                            () ->
                                    new ServiceException(
                                            "Could not get entry from store: " + doc.accession));
        } else {
            return getInactiveUniProtEntry(doc);
        }
    }

    private Optional<UniProtKBEntry> addLineageIfRequested(
            List<ReturnField> filters, UniProtKBEntry e) {
        if (hasLineage(filters)) {
            return addLineage(e);
        }
        return Optional.of(e);
    }

    boolean hasLineage(List<ReturnField> filters) {
        return filters.stream()
                .map(ReturnField::getName)
                .anyMatch(name -> "lineage".equals(name) || "lineage_ids".equals(name));
    }

    private Optional<UniProtKBEntry> addLineage(UniProtKBEntry entry) {
        TaxonomyEntry taxEntry = taxonomyLineageService.findById(entry.getOrganism().getTaxonId());
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

        String id = "";
        if (Utils.notNullNotEmpty(doc.id)) {
            id = doc.id.get(0);
        }
        UniProtKBId uniProtkbId = new UniProtKBIdBuilder(id).build();
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
