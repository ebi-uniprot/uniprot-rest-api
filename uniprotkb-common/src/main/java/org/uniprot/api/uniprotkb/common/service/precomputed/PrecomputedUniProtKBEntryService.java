package org.uniprot.api.uniprotkb.common.service.precomputed;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.rest.service.request.RequestConverter;
import org.uniprot.api.uniprotkb.common.repository.search.PrecomputedAnnotationRepository;
import org.uniprot.api.uniprotkb.common.repository.search.PrecomputedAnnotationSolrQueryConfig;
import org.uniprot.api.uniprotkb.common.repository.store.precomputed.PrecomputedAnnotationStoreClient;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.search.document.precomputed.PrecomputedAnnotationDocument;

@Service
@Import(PrecomputedAnnotationSolrQueryConfig.class)
public class PrecomputedUniProtKBEntryService {
    private final PrecomputedAnnotationStoreClient storeClient;
    private final PrecomputedAnnotationRepository repository;
    private final RequestConverter requestConverter;

    public PrecomputedUniProtKBEntryService(
            PrecomputedAnnotationStoreClient storeClient,
            PrecomputedAnnotationRepository repository,
            @Qualifier("precomputedAnnotationRequestConverter")
                    RequestConverter requestConverter) {
        this.storeClient = storeClient;
        this.repository = repository;
        this.requestConverter = requestConverter;
    }

    public UniProtKBEntry getPrecomputedUniProtKBEntry(String upi, String taxId) {
        String precomputedEntryId = upi + "-" + taxId;
        return this.storeClient
                .getEntry(precomputedEntryId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "No precomputed entry found for id: "
                                                + precomputedEntryId));
    }

    public QueryResult<UniProtKBEntry> search(PrecomputedAnnotationSearchByProteomeRequest request) {

        SolrRequest solrRequest = requestConverter.createSearchSolrRequest(request);
        QueryResult<PrecomputedAnnotationDocument> results = repository.searchPage(solrRequest, request.getCursor());
        List<String> accessions = results.getContent().map(PrecomputedAnnotationDocument::getAccession).toList();
        List<UniProtKBEntry> entries = storeClient.getEntries(accessions);

        return QueryResult.<UniProtKBEntry>builder()
                .content(entries.stream())
                .page(results.getPage())
                .build();
    }
}
