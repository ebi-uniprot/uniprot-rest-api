package org.uniprot.api.uniparc.common.service.light;

import static org.uniprot.api.uniparc.common.service.light.UniParcServiceUtils.csvToList;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.facet.Facet;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.common.repository.stream.store.BatchStoreIterable;
import org.uniprot.api.common.repository.stream.store.uniparc.UniParcCrossReferenceStoreConfigProperties;
import org.uniprot.api.uniparc.common.repository.store.crossref.UniParcCrossReferenceFacetConfig;
import org.uniprot.api.uniparc.common.repository.store.crossref.UniParcCrossReferenceStoreClient;
import org.uniprot.api.uniparc.common.repository.store.light.UniParcLightStoreClient;
import org.uniprot.api.uniparc.common.service.filter.UniParcCrossReferenceTaxonomyFilter;
import org.uniprot.api.uniparc.common.service.filter.UniParcDatabaseFilter;
import org.uniprot.api.uniparc.common.service.filter.UniParcDatabaseStatusFilter;
import org.uniprot.api.uniparc.common.service.request.UniParcDatabasesRequest;
import org.uniprot.api.uniparc.common.service.request.UniParcDatabasesStreamRequest;
import org.uniprot.api.uniparc.common.service.request.UniParcGetByIdRequest;
import org.uniprot.core.uniparc.UniParcCrossReference;
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
    private final UniParcCrossReferenceFacetConfig uniParcCrossReferenceFacetConfig;

    @Value("${search.default.page.size:#{null}}")
    private Integer defaultPageSize;

    @Autowired
    public UniParcCrossReferenceService(
            UniParcLightStoreClient uniParcLightStoreClient,
            UniParcCrossReferenceStoreClient uniParcCrossReferenceStoreClient,
            UniParcCrossReferenceStoreConfigProperties storeConfigProperties,
            UniParcCrossReferenceFacetConfig uniParcCrossReferenceFacetConfig) {
        this.uniParcLightStoreClient = uniParcLightStoreClient;
        this.crossReferenceStoreClient = uniParcCrossReferenceStoreClient;
        this.crossReferenceStoreRetryPolicy =
                new RetryPolicy<>()
                        .handle(IOException.class)
                        .withDelay(
                                Duration.ofMillis(storeConfigProperties.getFetchRetryDelayMillis()))
                        .withMaxRetries(storeConfigProperties.getFetchMaxRetries());
        this.storeConfigProperties = storeConfigProperties;
        this.uniParcCrossReferenceFacetConfig = uniParcCrossReferenceFacetConfig;
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
                    getFilteredCrossReferences(uniParcEntryLight, request).toList();
            page = CursorPage.of(request.getCursor(), pageSize, filteredCrossReferences.size());
            paginatedResults = paginateCrossReferences(page, filteredCrossReferences);
        } else {
            int xrefCount = uniParcEntryLight.getCrossReferenceCount();
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
        // populate facets if needed
        List<Facet> facets = null;
        if (facetNeeded(request)) {
            Stream<UniParcCrossReference> allCrossReferences =
                    getFilteredCrossReferences(uniParcEntryLight, request);
            facets =
                    this.uniParcCrossReferenceFacetConfig.getUniParcCrossReferenceFacets(
                            allCrossReferences, request.getFacets());
        }

        return QueryResult.<UniParcCrossReference>builder()
                .content(paginatedResults.stream())
                .page(page)
                .facets(facets)
                .build();
    }

    public Stream<UniParcCrossReference> streamCrossReferences(
            String uniParcId, UniParcDatabasesStreamRequest streamRequest) {
        Optional<UniParcEntryLight> optUniParcLight =
                this.uniParcLightStoreClient.getEntry(uniParcId);
        if (optUniParcLight.isEmpty()) {
            throw new ResourceNotFoundException("Unable to find UniParc id " + uniParcId);
        }
        UniParcEntryLight uniParcEntryLight = optUniParcLight.get();
        return getFilteredCrossReferences(uniParcEntryLight, streamRequest);
    }

    public Stream<UniParcCrossReference> getCrossReferences(UniParcEntryLight uniParcEntryLight) {
        BatchStoreIterable<UniParcCrossReferencePair> batchIterable =
                new BatchStoreIterable<>(
                        generateUniParcCrossReferenceKeys(uniParcEntryLight),
                        this.crossReferenceStoreClient,
                        this.crossReferenceStoreRetryPolicy,
                        1);
        return StreamSupport.stream(batchIterable.spliterator(), false)
                .flatMap(Collection::stream)
                .flatMap(pair -> pair.getValue().stream());
    }

    private Stream<UniParcCrossReference> getFilteredCrossReferences(
            UniParcEntryLight uniParcEntryLight, UniParcGetByIdRequest request) {

        int groupSize = this.storeConfigProperties.getGroupSize();
        int crossReferencesCount = uniParcEntryLight.getCrossReferenceCount();
        // Calculate the number of batches required
        int storePageCount =
                crossReferencesCount / groupSize + (crossReferencesCount % groupSize == 0 ? 0 : 1);
        String uniParcId = uniParcEntryLight.getUniParcId();
        return IntStream.range(0, storePageCount)
                .mapToObj(batch -> uniParcId + "_" + batch)
                .map(this.crossReferenceStoreClient::getEntry)
                .flatMap(Optional::stream)
                .flatMap(pair -> pair.getValue().stream())
                .filter(xref -> filterCrossReference(xref, request));
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

    private boolean hasRequestFilters(UniParcGetByIdRequest request) {
        return Utils.notNullNotEmpty(request.getTaxonIds())
                || Objects.nonNull(request.getActive())
                || Utils.notNullNotEmpty(request.getDbTypes());
    }

    private List<UniParcCrossReference> paginateCrossReferences(
            CursorPage page, List<UniParcCrossReference> xrefs) {
        int offset = page.getOffset().intValue();
        int nextOffset = CursorPage.getNextOffset(page);
        return xrefs.subList(offset, nextOffset);
    }

    private boolean filterCrossReference(
            UniParcCrossReference xref, UniParcGetByIdRequest request) {
        List<String> databases =
                csvToList(request.getDbTypes()).stream().map(String::toLowerCase).toList();
        List<String> taxonomyIds = csvToList(request.getTaxonIds());
        UniParcDatabaseFilter dbFilter = new UniParcDatabaseFilter();
        UniParcCrossReferenceTaxonomyFilter taxonFilter = new UniParcCrossReferenceTaxonomyFilter();
        UniParcDatabaseStatusFilter statusFilter = new UniParcDatabaseStatusFilter();

        return dbFilter.apply(xref, databases)
                && taxonFilter.apply(xref, taxonomyIds)
                && statusFilter.apply(xref, request.getActive());
    }

    private int getDefaultPageSize() {
        return this.defaultPageSize;
    }

    private List<String> generateUniParcCrossReferenceKeys(UniParcEntryLight uniParcEntryLight) {
        int groupSize = this.storeConfigProperties.getGroupSize();
        int xrefCount = uniParcEntryLight.getCrossReferenceCount();
        List<String> xrefKeys = new ArrayList<>();
        String uniParcId = uniParcEntryLight.getUniParcId();
        for (int i = 0, batchId = 0; i < xrefCount; i += groupSize, batchId++) {
            xrefKeys.add(uniParcId + "_" + batchId);
        }
        return xrefKeys;
    }

    private boolean facetNeeded(UniParcDatabasesRequest request) {
        return !request.hasCursor() && Utils.notNullNotEmpty(request.getFacets());
    }
}
