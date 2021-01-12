package org.uniprot.api.uniprotkb.service;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.solr.client.solrj.SolrQuery;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.QueryOperator;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.rest.service.query.QueryProcessor;
import org.uniprot.api.uniprotkb.controller.request.PublicationRequest;
import org.uniprot.api.uniprotkb.model.PublicationEntry;
import org.uniprot.api.uniprotkb.repository.search.impl.LiteratureRepository;
import org.uniprot.api.uniprotkb.repository.search.impl.PublicationRepository;
import org.uniprot.api.uniprotkb.repository.search.impl.PublicationSolrQueryConfig;
import org.uniprot.core.citation.Citation;
import org.uniprot.core.citation.Literature;
import org.uniprot.core.literature.LiteratureEntry;
import org.uniprot.core.literature.LiteratureStoreEntry;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.search.document.literature.LiteratureDocument;
import org.uniprot.store.search.document.publication.PublicationDocument;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created 06/01/2021
 *
 * @author Edd
 */
@Service
@Slf4j
@Import(PublicationSolrQueryConfig.class)
public class PublicationService
        extends BasicSearchService<PublicationDocument, PublicationEntry> {
    private final PublicationRepository publicationRepository;
    private final LiteratureRepository literatureRepository;
    private final PublicationConverter publicationConverter;
    private final QueryProcessor publicationQueryProcessor;
    private final LiteratureStoreEntryConverter literatureEntryStoreConverter;
    private final String idFieldName;
    private final String accessionFieldName;

    public PublicationService(
            PublicationRepository publicationRepository,
            LiteratureRepository literatureRepository,
            PublicationConverter publicationConverter,
            UniProtKBPublicationsSolrSortClause solrSortClause,
            LiteratureStoreEntryConverter literatureEntryStoreConverter,
            @Qualifier("publicationQueryConfig") SolrQueryConfig publicationSolrQueryConf,
            PublicationFacetConfig facetConfig,
            QueryProcessor publicationQueryProcessor) {
        super(publicationRepository, null, solrSortClause, publicationSolrQueryConf, facetConfig);
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
    }

    @Override
    public QueryResult<PublicationEntry> search(SearchRequest request) {
        SolrRequest pubDocsForAccessionSolrRequest = createSearchSolrRequest(request);

        QueryResult<PublicationDocument> results =
                publicationRepository.searchPage(
                        pubDocsForAccessionSolrRequest, request.getCursor());
        List<PublicationDocument> content = results.getContent().collect(Collectors.toList());

        Map<Long, Citation> citationMap = getPubMedCitationMap(content);

        Stream<PublicationEntry> converted =
                content.stream()
                        .map(e -> publicationConverter.apply(e, citationMap))
                        .filter(Objects::nonNull);

        return QueryResult.of(converted, results.getPage(), results.getFacets());
    }

    public QueryResult<PublicationEntry> getPublicationsByUniProtAccession(
            final String accession, PublicationRequest request) {
        PublicationSearchRequest searchRequest =
                PublicationSearchRequest.builder()
                        .query(accessionFieldName + ":" + accession)
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

    private Map<Long, Citation> getPubMedCitationMap(List<PublicationDocument> content) {
        BooleanQuery.Builder pubmedIdsQuery = new BooleanQuery.Builder();
        content.stream()
                .map(PublicationDocument::getPubMedId)
                .filter(Objects::nonNull)
                .forEach(
                        pubmedId ->
                                pubmedIdsQuery.add(
                                        new TermQuery(new Term(idFieldName, pubmedId)),
                                        BooleanClause.Occur.SHOULD));
        SolrRequest pubmedIdsSolrRequest = getSolrRequest(pubmedIdsQuery.build().toString());
        Stream<LiteratureDocument> all = literatureRepository.getAll(pubmedIdsSolrRequest);
        return all.map(literatureEntryStoreConverter)
                .map(LiteratureStoreEntry::getLiteratureEntry)
                .collect(
                        Collectors.toMap(this::getPubmedIdFromEntry, LiteratureEntry::getCitation));
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

    @Builder
    @Data
    private static class PublicationSearchRequest implements SearchRequest {
        private String query;
        private String sort;
        private String cursor;
        private String fields;
        private Integer size;
        private String facets;

        @Override
        public List<String> getFacetList() {
            if (Utils.notNullNotEmpty(facets)) {
                return Arrays.asList(facets.split(("\\s*,\\s*")));
            } else {
                return Collections.emptyList();
            }
        }
    }
}
