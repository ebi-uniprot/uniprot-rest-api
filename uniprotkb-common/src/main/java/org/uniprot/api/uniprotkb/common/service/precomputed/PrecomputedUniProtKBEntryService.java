package org.uniprot.api.uniprotkb.common.service.precomputed;

import static org.uniprot.api.rest.output.UniProtMediaType.LIST_MEDIA_TYPE_VALUE;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.stream.document.TupleStreamDocumentIdStream;
import org.uniprot.api.common.repository.stream.store.StoreRequest;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.rest.service.request.RequestConverter;
import org.uniprot.api.uniprotkb.common.repository.search.PrecomputedAnnotationRepository;
import org.uniprot.api.uniprotkb.common.repository.search.PrecomputedAnnotationSolrQueryConfig;
import org.uniprot.api.uniprotkb.common.repository.store.precomputed.PrecomputedAnnotationStoreClient;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.uniprotkb.UniProtKBEntryType;
import org.uniprot.core.uniprotkb.impl.UniProtKBEntryBuilder;
import org.uniprot.store.search.document.precomputed.PrecomputedAnnotationDocument;

@Service
@Import(PrecomputedAnnotationSolrQueryConfig.class)
public class PrecomputedUniProtKBEntryService {
    private final PrecomputedAnnotationStoreClient storeClient;
    private final PrecomputedAnnotationRepository repository;
    private final ProteomeTaxonomyResolver proteomeTaxonomyResolver;
    private final RequestConverter requestConverter;
    private final StoreStreamer<UniProtKBEntry> storeStreamer;
    private final TupleStreamDocumentIdStream solrIdStreamer;

    public PrecomputedUniProtKBEntryService(
            PrecomputedAnnotationStoreClient storeClient,
            PrecomputedAnnotationRepository repository,
            ProteomeTaxonomyResolver proteomeTaxonomyResolver,
            @Qualifier("precomputedAnnotationRequestConverter") RequestConverter requestConverter,
            @Qualifier("precomputedAnnotationStoreStreamer")
                    StoreStreamer<UniProtKBEntry> storeStreamer,
            @Qualifier("precomputedAnnotationTupleStreamDocumentIdStream")
                    TupleStreamDocumentIdStream solrIdStreamer) {
        this.storeClient = storeClient;
        this.repository = repository;
        this.proteomeTaxonomyResolver = proteomeTaxonomyResolver;
        this.requestConverter = requestConverter;
        this.storeStreamer = storeStreamer;
        this.solrIdStreamer = solrIdStreamer;
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

    public QueryResult<UniProtKBEntry> search(
            PrecomputedAnnotationSearchByProteomeRequest request) {
        String taxonomyId = proteomeTaxonomyResolver.findTaxonomyIdByUpId(request.getUpId());
        request.setTaxonomyId(taxonomyId);

        SolrRequest solrRequest = requestConverter.createSearchSolrRequest(request);
        QueryResult<PrecomputedAnnotationDocument> results =
                repository.searchPage(solrRequest, request.getCursor());
        List<String> accessions =
                results.getContent().map(PrecomputedAnnotationDocument::getAccession).toList();
        List<UniProtKBEntry> entries = storeClient.getEntries(accessions);

        return QueryResult.<UniProtKBEntry>builder()
                .content(entries.stream())
                .page(results.getPage())
                .suggestions(results.getSuggestions())
                .warnings(results.getWarnings())
                .build();
    }

    public Stream<UniProtKBEntry> streamByProteomeId(
            PrecomputedAnnotationStreamByProteomeRequest request) {
        String taxonomyId = proteomeTaxonomyResolver.findTaxonomyIdByUpId(request.getUpId());
        request.setTaxonomyId(taxonomyId);

        SolrRequest solrRequest = requestConverter.createStreamSolrRequest(request);
        if (LIST_MEDIA_TYPE_VALUE.equals(request.getFormat())) {
            return this.solrIdStreamer
                    .fetchIds(solrRequest)
                    .map(this::mapToThinEntry)
                    .filter(Objects::nonNull);
        } else {
            StoreRequest storeRequest = StoreRequest.builder().fields(request.getFields()).build();
            return this.storeStreamer.idsToStoreStream(solrRequest, storeRequest);
        }
    }

    protected UniProtKBEntry mapToThinEntry(String accession) {
        UniProtKBEntryBuilder builder =
                new UniProtKBEntryBuilder(accession, accession, UniProtKBEntryType.SWISSPROT);
        return builder.build();
    }
}
