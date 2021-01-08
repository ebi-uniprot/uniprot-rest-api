package org.uniprot.api.uniprotkb.service;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.solr.client.solrj.SolrQuery;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.QueryOperator;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.rest.service.query.QueryProcessor;
import org.uniprot.api.uniprotkb.controller.request.PublicationRequest;
import org.uniprot.api.uniprotkb.model.PublicationEntry2;
import org.uniprot.api.uniprotkb.repository.search.impl.LiteratureRepository;
import org.uniprot.api.uniprotkb.repository.search.impl.PublicationRepository;
import org.uniprot.core.citation.Citation;
import org.uniprot.core.citation.Literature;
import org.uniprot.core.literature.LiteratureEntry;
import org.uniprot.core.literature.LiteratureStoreEntry;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.search.document.literature.LiteratureDocument;
import org.uniprot.store.search.document.publication.PublicationDocument;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created 06/01/2021
 *
 * @author Edd
 */
@Service
@Slf4j
public class PublicationService2
        extends BasicSearchService<PublicationDocument, PublicationEntry2> {
    private final PublicationRepository publicationRepository;
    private final LiteratureRepository literatureRepository;
    private final PublicationConverter publicationConverter;
    private final QueryProcessor publicationQueryProcessor;
    private final LiteratureStoreEntryConverter literatureEntryStoreConverter;
    private final String idFieldName;
    private final String sortCriteria;
    private final String accessionFieldName;

    public PublicationService2(
            PublicationRepository publicationRepository,
            LiteratureRepository literatureRepository,
            PublicationConverter publicationConverter,
            LiteratureStoreEntryConverter literatureEntryStoreConverter,
            SolrQueryConfig queryBoosts,
            PublicationFacetConfig facetConfig,
            QueryProcessor publicationQueryProcessor) {
        super(publicationRepository, null, null, queryBoosts, facetConfig);
        this.publicationRepository = publicationRepository;
        this.literatureRepository = literatureRepository;
        this.publicationConverter = publicationConverter;
        this.publicationQueryProcessor = publicationQueryProcessor;
        this.literatureEntryStoreConverter = literatureEntryStoreConverter;
        this.idFieldName =
                SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.LITERATURE)
                        .getSearchFieldItemByName("id")
                        .getFieldName();
        this.accessionFieldName =
                SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.PUBLICATION)
                        .getSearchFieldItemByName("accession")
                        .getFieldName();
        this.sortCriteria = getPublicationsSort();
    }

    @Override
    public QueryResult<PublicationEntry2> search(SearchRequest request) {
        SolrRequest solrRequest = createSearchSolrRequest(request);

        QueryResult<PublicationDocument> results =
                publicationRepository.searchPage(solrRequest, request.getCursor());

        BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
        List<PublicationDocument> content = results.getContent().collect(Collectors.toList());
        content.stream()
                .map(PublicationDocument::getPubMedId)
                .filter(Objects::nonNull)
                .forEach(
                        pubmedId ->
                                queryBuilder.add(
                                        new TermQuery(new Term(idFieldName, pubmedId)),
                                        BooleanClause.Occur.SHOULD));
        SolrRequest solrRequest1 = getSolrRequest(queryBuilder.build().toString());
        Stream<LiteratureDocument> all = literatureRepository.getAll(solrRequest1);
        Map<Long, Citation> citationMap =
                all.map(literatureEntryStoreConverter)
                        .map(LiteratureStoreEntry::getLiteratureEntry)
                        .collect(
                                Collectors.toMap(
                                        this::getPubmedIdFromEntry, LiteratureEntry::getCitation));

        Stream<PublicationEntry2> converted =
                content.stream()
                        .map(e -> publicationConverter.apply(e, citationMap))
                        .filter(Objects::nonNull);

        return QueryResult.of(converted, results.getPage(), results.getFacets());
    }

    public QueryResult<PublicationEntry2> getPublicationsByUniProtAccession(
            final String accession, PublicationRequest request) {
        PublicationSearchRequest searchRequest =
                PublicationSearchRequest.builder()
                        .query(accessionFieldName + ":" + accession)
                        .sort(sortCriteria)
                        .size(request.getSize())
                        .cursor(request.getCursor())
                        .facets(request.getFacets())
                        .build();

        return search(searchRequest);
    }

    @Override
    protected SearchFieldItem getIdField() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected QueryProcessor getQueryProcessor() {
        return publicationQueryProcessor;
    }

    private Long getPubmedIdFromEntry(LiteratureEntry entry) {
        Literature literature = (Literature) entry.getCitation();
        return literature.getPubmedId();
    }

    private SolrRequest getSolrRequest(String query) {
        return SolrRequest.builder()
                .query(query)
                .sort(SolrQuery.SortClause.asc(idFieldName))
                .defaultQueryOperator(QueryOperator.OR)
                .rows(100)
                .build();
    }

    // main_type desc,reference_number asc,pubmed_id desc
    private String getPublicationsSort() {
        return getSortCriterion(
                        SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.PUBLICATION)
                                .getSearchFieldItemByName("main_type")
                                .getFieldName(),
                        "desc")
                + ','
                + getSortCriterion(
                        SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.PUBLICATION)
                                .getSearchFieldItemByName("reference_number")
                                .getFieldName(),
                        "asc")
                + ','
                + getSortCriterion(
                        SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.PUBLICATION)
                                .getSearchFieldItemByName("pubmed_id")
                                .getFieldName(),
                        "desc");
    }

    private String getSortCriterion(String field, String order) {
        return field + " " + order;
    }

    @Builder
    @Data
    private static class PublicationSearchRequest implements SearchRequest {
        private String query;
        private String sort;
        private String cursor;
        private String fields;
        private Integer size;
        private String facets;
    }
}
