package org.uniprot.api.literature.service;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.literature.repository.LiteratureFacetConfig;
import org.uniprot.api.literature.repository.LiteratureRepository;
import org.uniprot.api.literature.request.LiteratureMappedRequestDTO;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.core.literature.LiteratureEntry;
import org.uniprot.store.search.DefaultSearchHandler;
import org.uniprot.store.search.document.literature.LiteratureDocument;
import org.uniprot.store.search.field.LiteratureField;

/**
 * @author lgonzales
 * @since 2019-07-04
 */
@Service
public class LiteratureService extends BasicSearchService<LiteratureDocument, LiteratureEntry> {

    private static final Supplier<DefaultSearchHandler> handlerSupplier =
            () ->
                    new DefaultSearchHandler(
                            LiteratureField.Search.content,
                            LiteratureField.Search.id,
                            LiteratureField.Search.getBoostFields());

    @Autowired private LiteratureSortClause literatureSortClause;
    @Autowired private LiteratureRepository repository;
    @Autowired private LiteratureEntryConverter entryConverter;

    public LiteratureService(
            LiteratureRepository repository,
            LiteratureEntryConverter entryConverter,
            LiteratureFacetConfig facetConfig,
            LiteratureSortClause literatureSortClause) {
        super(repository, entryConverter, literatureSortClause, handlerSupplier.get(), facetConfig);
    }

    @Override
    public LiteratureEntry findByUniqueId(String uniqueId) {
        return getEntity(LiteratureField.Search.id.name(), uniqueId);
    }

    public QueryResult<LiteratureEntry> getMappedLiteratureByUniprotAccession(
            final String accession, LiteratureMappedRequestDTO requestDTO) {
        SolrRequest solrRequest =
                SolrRequest.builder()
                        .query("mapped_protein:" + accession)
                        .addSort(literatureSortClause.getSort(requestDTO.getSort(), false))
                        .build();
        QueryResult<LiteratureDocument> results =
                repository.searchPage(solrRequest, requestDTO.getCursor(), requestDTO.getSize());

        List<LiteratureEntry> converted =
                results.getContent().stream()
                        .map(
                                literatureDocument ->
                                        convertDocumentToEntryAndFilterMappedAccession(
                                                literatureDocument, accession))
                        .collect(Collectors.toList());
        return QueryResult.of(converted, results.getPage(), results.getFacets());
    }

    private LiteratureEntry convertDocumentToEntryAndFilterMappedAccession(
            LiteratureDocument literatureDocument, String accession) {
        LiteratureEntry entry = entryConverter.apply(literatureDocument);
        entry.getLiteratureMappedReferences()
                .removeIf(
                        mappedReference ->
                                !mappedReference
                                        .getUniprotAccession()
                                        .getValue()
                                        .equalsIgnoreCase(accession));
        return entry;
    }
}
