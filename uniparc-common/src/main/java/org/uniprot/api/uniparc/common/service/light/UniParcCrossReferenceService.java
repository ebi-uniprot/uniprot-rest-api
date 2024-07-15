package org.uniprot.api.uniparc.common.service.light;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
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
import org.uniprot.core.uniparc.UniParcEntryLight;
import org.uniprot.core.util.Utils;

import net.jodah.failsafe.RetryPolicy;

@Service
public class UniParcCrossReferenceService {

    private final UniParcLightStoreClient uniParcLightStoreClient;
    private final UniParcCrossReferenceStoreClient crossReferenceStoreClient;
    private final RetryPolicy<Object> crossReferenceStoreRetryPolicy;

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
    }

    public QueryResult<UniParcCrossReference> getCrossReferencesByUniParcId(
            String uniParcId, UniParcDatabasesRequest request) {
        Optional<UniParcEntryLight> optUniParcLight =
                this.uniParcLightStoreClient.getEntry(uniParcId);
        if (optUniParcLight.isEmpty()) {
            throw new ResourceNotFoundException("Unable to find UniParc id " + uniParcId);
        }
        UniParcEntryLight entry = optUniParcLight.get();
        List<String> xrefIds = entry.getUniParcCrossReferences();

        // Fetch, filter, and paginate
        List<UniParcCrossReference> filteredResults = new ArrayList<>();
        for (int i = 0; i < xrefIds.size(); i += this.crossReferenceStoreClient.getBatchSize()) {
            int end = Math.min(i + this.crossReferenceStoreClient.getBatchSize(), xrefIds.size());
            List<String> batch = xrefIds.subList(i, end);
            Stream<UniParcCrossReference> batchStream = getCrossReferences(batch);
            batchStream
                    .filter(xref -> filterCrossReference(xref, request))
                    .forEach(filteredResults::add);
        }

        // Apply pagination
        int pageSize = Objects.isNull(request.getSize()) ? getDefaultPageSize() : request.getSize();
        CursorPage page = CursorPage.of(request.getCursor(), pageSize, filteredResults.size());
        int offset = page.getOffset().intValue();
        int nextOffset = CursorPage.getNextOffset(page);
        List<UniParcCrossReference> paginatedResults = filteredResults.subList(offset, nextOffset);

        return QueryResult.<UniParcCrossReference>builder()
                .content(paginatedResults.stream())
                .page(page)
                .build();
    }

    private Stream<UniParcCrossReference> getCrossReferences(List<String> xrefIds) {
        BatchStoreIterable<UniParcCrossReference> batchIterable =
                new BatchStoreIterable<>(
                        xrefIds,
                        this.crossReferenceStoreClient,
                        this.crossReferenceStoreRetryPolicy,
                        this.crossReferenceStoreClient.getBatchSize());
        return StreamSupport.stream(batchIterable.spliterator(), false).flatMap(Collection::stream);
    }

    private boolean filterCrossReference(
            UniParcCrossReference xref, UniParcDatabasesRequest request) {
        List<String> databases = csvToList(request.getDbTypes());
        List<String> toxonomyIds = csvToList(request.getTaxonIds());
        return filterByDatabases(xref, databases)
                && filterByTaxonomyIds(xref, toxonomyIds)
                && filterByStatus(xref, request.getActive());
    }

    private boolean filterByDatabases(UniParcCrossReference xref, List<String> databases) {
        if (Utils.nullOrEmpty(databases)) {
            return true;
        }

        return Objects.nonNull(xref.getDatabase()) && databases.contains(xref.getDatabase().getDisplayName().toLowerCase());
    }

    private boolean filterByTaxonomyIds(UniParcCrossReference xref, List<String> taxonomyIds) {
        if (Utils.nullOrEmpty(taxonomyIds)) {
            return true;
        }

        return Objects.nonNull(xref.getOrganism()) && taxonomyIds.contains(String.valueOf(xref.getOrganism().getTaxonId()));
    }

    private boolean filterByStatus(UniParcCrossReference xref, Boolean isActive) {
        if (isActive == null) {
            return true;
        }
        return Objects.nonNull(xref.getDatabase()) && Objects.equals(isActive, xref.isActive());
    }

    private List<String> csvToList(String csv) {
        List<String> list = new ArrayList<>();
        if (Utils.notNullNotEmpty(csv)) {
            list =
                    Arrays.stream(csv.split(","))
                            .map(String::trim)
                            .map(String::toLowerCase)
                            .collect(Collectors.toList());
        }
        return list;
    }

    private int getDefaultPageSize() {
        return this.defaultPageSize;
    }
}
