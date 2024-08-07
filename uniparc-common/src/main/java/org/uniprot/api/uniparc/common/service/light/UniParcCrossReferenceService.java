package org.uniprot.api.uniparc.common.service.light;

import static org.uniprot.api.uniparc.common.service.light.UniParcServiceUtils.*;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.common.repository.stream.store.BatchStoreIterable;
import org.uniprot.api.uniparc.common.repository.store.crossref.UniParcCrossReferenceStoreClient;
import org.uniprot.api.uniparc.common.repository.store.crossref.UniParcCrossReferenceStoreConfigProperties;
import org.uniprot.api.uniparc.common.repository.store.light.UniParcLightStoreClient;
import org.uniprot.api.uniparc.common.service.request.UniParcDatabasesRequest;
import org.uniprot.core.uniparc.UniParcCrossReference;
import org.uniprot.core.uniparc.UniParcDatabase;
import org.uniprot.core.uniparc.UniParcEntryLight;
import org.uniprot.core.uniparc.impl.UniParcCrossReferencePair;
import org.uniprot.core.util.Utils;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.RetryPolicy;

@Slf4j
@Service
public class UniParcCrossReferenceService {

    private final UniParcLightStoreClient uniParcLightStoreClient;
    private final UniParcCrossReferenceStoreClient crossReferenceStoreClient;
    private final RetryPolicy<Object> crossReferenceStoreRetryPolicy;
    private final UniParcCrossReferenceStoreConfigProperties storeConfigProperties;

    @Value("${search.default.page.size:#{null}}")
    private Integer defaultPageSize;

    @Autowired
    public UniParcCrossReferenceService(
            UniParcLightStoreClient uniParcLightStoreClient,
            UniParcCrossReferenceStoreClient crossReferenceStoreClient,
            UniParcCrossReferenceStoreConfigProperties storeConfigProperties) {
        this.uniParcLightStoreClient = uniParcLightStoreClient;
        this.crossReferenceStoreClient = crossReferenceStoreClient;
        this.crossReferenceStoreRetryPolicy =
                new RetryPolicy<>()
                        .handle(IOException.class)
                        .withDelay(
                                Duration.ofMillis(storeConfigProperties.getFetchRetryDelayMillis()))
                        .withMaxRetries(storeConfigProperties.getFetchMaxRetries());
        this.storeConfigProperties = storeConfigProperties;
    }

    public QueryResult<UniParcCrossReference> getCrossReferencesByUniParcId(
            String uniParcId, UniParcDatabasesRequest request) {
        Optional<UniParcEntryLight> optUniParcLight =
                this.uniParcLightStoreClient.getEntry(uniParcId);
        if (optUniParcLight.isEmpty()) {
            throw new ResourceNotFoundException("Unable to find UniParc id " + uniParcId);
        }
        UniParcEntryLight uniParcEntryLight = optUniParcLight.get();
        int pageSize = Objects.isNull(request.getSize()) ? getDefaultPageSize() : request.getSize();
        List<UniParcCrossReference> paginatedResults;
        CursorPage page;
        if (hasRequestFilters(request)) {
            List<UniParcCrossReference> filteredCrossReferences =
                    getFilteredCrossReferences(uniParcEntryLight, request);
            page = CursorPage.of(request.getCursor(), pageSize, filteredCrossReferences.size());
            paginatedResults = paginateCrossReferences(page, filteredCrossReferences);
        } else {
            int xrefCount = uniParcEntryLight.getNumberOfUniParcCrossReferences();
            page = CursorPage.of(request.getCursor(), pageSize, xrefCount);
            List<UniParcCrossReference> requiredCrossReferenceBatches =
                    getRequiredCrossReferenceBatches(uniParcEntryLight, page);
            // start offset in the current candidates
            int offset = page.getOffset().intValue();
            int nextOffset = CursorPage.getNextOffset(page);
            int scaledOffset = offset % this.storeConfigProperties.getGroupSize();
            // end index for sublist
            int scaledNextOffset = scaledOffset + nextOffset - offset;
            scaledNextOffset = Math.min(scaledNextOffset, requiredCrossReferenceBatches.size());
            paginatedResults =
                    requiredCrossReferenceBatches.subList(scaledOffset, scaledNextOffset);
        }
        return QueryResult.<UniParcCrossReference>builder()
                .content(paginatedResults.stream())
                .page(page)
                .build();
    }

    private List<UniParcCrossReference> getFilteredCrossReferences(
            UniParcEntryLight uniParcEntryLight, UniParcDatabasesRequest request) {
        List<UniParcCrossReference> filteredCrossReferences = new ArrayList<>();
        int groupSize = this.storeConfigProperties.getGroupSize();
        int crossReferencesCount = uniParcEntryLight.getNumberOfUniParcCrossReferences();
        // Calculate the number of batches required
        int storePageCount =
                crossReferencesCount / groupSize + (crossReferencesCount % groupSize == 0 ? 0 : 1);
        for (int i = 0; i < storePageCount; i++) {
            String xrefPageId = uniParcEntryLight.getUniParcId() + "_" + i;
            Stream<UniParcCrossReference> filteredCrossReferencesBatch =
                    this.crossReferenceStoreClient
                            .getEntry(xrefPageId)
                            .map(UniParcCrossReferencePair::getValue)
                            .stream()
                            .flatMap(Collection::stream)
                            .filter(xref -> filterCrossReference(xref, request));
            filteredCrossReferences.addAll(filteredCrossReferencesBatch.toList());
        }
        return filteredCrossReferences;
    }

    private List<UniParcCrossReference> getRequiredCrossReferenceBatches(
            UniParcEntryLight entry, CursorPage page) {
        int groupSize = this.storeConfigProperties.getGroupSize();
        int offset = page.getOffset().intValue();
        int nextOffset = CursorPage.getNextOffset(page);
        // start batch where first or first few xrefs are
        int startBatchIndex = offset / groupSize;
        // end batch where last or last few xrefs are or even end index of sublist lies
        int endBatchIndex = (nextOffset - 1) / groupSize;
        int estimatedCapacity = (endBatchIndex - startBatchIndex + 1) * groupSize;
        List<UniParcCrossReference> requiredCrossReferenceBatches =
                new ArrayList<>(estimatedCapacity);
        // get all the xrefs from start to end batches
        for (int i = startBatchIndex; i <= endBatchIndex; i++) {
            String xrefBatchId = entry.getUniParcId() + "_" + i;
            List<UniParcCrossReference> crossReferencesBatch =
                    this.crossReferenceStoreClient
                            .getEntry(xrefBatchId)
                            .map(UniParcCrossReferencePair::getValue)
                            .orElse(List.of());
            requiredCrossReferenceBatches.addAll(crossReferencesBatch);
        }
        return requiredCrossReferenceBatches;
    }

    private boolean hasRequestFilters(UniParcDatabasesRequest request) {
        return Utils.notNullNotEmpty(request.getTaxonIds())
                || Objects.nonNull(request.getActive())
                || Utils.notNullNotEmpty(request.getDbTypes());
    }

    /*    private List<UniParcCrossReference> paginateAndFetchResults(
            CursorPage page, List<String> xrefIds) {
        int offset = page.getOffset().intValue();
        int nextOffset = CursorPage.getNextOffset(page);
        List<String> pageXrefIds = xrefIds.subList(offset, nextOffset);
        return getCrossReferences(pageXrefIds).toList();
    }*/

    private List<UniParcCrossReference> paginateCrossReferences(
            CursorPage page, List<UniParcCrossReference> xrefs) {
        int offset = page.getOffset().intValue();
        int nextOffset = CursorPage.getNextOffset(page);
        return xrefs.subList(offset, nextOffset);
    }

    // example xrefId key in VD - UPI0000000001-REFSEQ-12345-3
    private static boolean filterXrefIdByDbTypes(String xrefId, List<String> dbTypes) {
        String[] parts = xrefId.split("-");
        if (parts.length > 1) {
            try {
                String displayName =
                        UniParcDatabase.valueOf(parts[1]).getDisplayName().toLowerCase();
                return dbTypes.contains(displayName);
            } catch (IllegalArgumentException ile) {
                log.warn("Unable to find UniParc database of type {}", parts[1]);
                return false;
            }
        }
        return false;
    }

    public Stream<UniParcCrossReference> getCrossReferences(int numberOfXrefs) {
        BatchStoreIterable<UniParcCrossReferencePair> batchIterable =
                new BatchStoreIterable<>(
                        List.of(), // TODO: we need to implement BatchStoreIterable
                        this.crossReferenceStoreClient,
                        this.crossReferenceStoreRetryPolicy,
                        1);
        return StreamSupport.stream(batchIterable.spliterator(), false)
                .flatMap(Collection::stream)
                .flatMap(pair -> pair.getValue().stream());
    }

    private boolean filterCrossReference(
            UniParcCrossReference xref, UniParcDatabasesRequest request) {
        List<String> databases =
                csvToList(request.getDbTypes()).stream().map(String::toLowerCase).toList();
        ;
        List<String> taxonomyIds = csvToList(request.getTaxonIds());
        return filterByDatabases(xref, databases)
                && filterByTaxonomyIds(xref, taxonomyIds)
                && filterByStatus(xref, request.getActive());
    }

    private int getDefaultPageSize() {
        return this.defaultPageSize;
    }
}
