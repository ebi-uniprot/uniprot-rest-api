package org.uniprot.api.uniprotkb.common.service.publication;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.solr.client.solrj.SolrQuery;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.ProblemPair;
import org.uniprot.api.common.repository.search.QueryOperator;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.rest.service.query.config.LiteratureSolrQueryConfig;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
import org.uniprot.api.rest.service.request.RequestConverter;
import org.uniprot.api.uniprotkb.common.repository.model.PublicationEntry;
import org.uniprot.api.uniprotkb.common.repository.search.LiteratureRepository;
import org.uniprot.api.uniprotkb.common.repository.search.PublicationRepository;
import org.uniprot.api.uniprotkb.common.repository.search.PublicationSolrQueryConfig;
import org.uniprot.api.uniprotkb.common.service.publication.request.PublicationRequest;
import org.uniprot.core.literature.LiteratureEntry;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.search.document.literature.LiteratureDocument;
import org.uniprot.store.search.document.publication.PublicationDocument;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Created 06/01/2021
 *
 * @author Edd
 */
@Service
@Slf4j
@Import({PublicationSolrQueryConfig.class, LiteratureSolrQueryConfig.class})
public class PublicationService extends BasicSearchService<PublicationDocument, PublicationEntry> {
    private final PublicationRepository publicationRepository;
    private final LiteratureRepository literatureRepository;
    private final PublicationConverter publicationConverter;
    private final UniProtQueryProcessorConfig literatureQueryProcessorConfig;
    private final LiteratureEntryConverter literatureEntryConverter;
    private final SolrQueryConfig literatureSolrQueryConf;
    private final RequestConverter publicationRequestConverter;
    private final String idFieldName;
    private final String accessionFieldName;

    public PublicationService(
            PublicationRepository publicationRepository,
            LiteratureRepository literatureRepository,
            PublicationConverter publicationConverter,
            LiteratureEntryConverter literatureEntryConverter,
            UniProtQueryProcessorConfig literatureQueryProcessorConfig,
            SolrQueryConfig literatureSolrQueryConf,
            RequestConverter publicationRequestConverter) {
        super(publicationRepository, null, publicationRequestConverter);
        this.publicationRepository = publicationRepository;
        this.literatureRepository = literatureRepository;
        this.publicationConverter = publicationConverter;
        this.literatureQueryProcessorConfig = literatureQueryProcessorConfig;
        this.literatureEntryConverter = literatureEntryConverter;
        this.literatureSolrQueryConf = literatureSolrQueryConf;
        this.publicationRequestConverter = publicationRequestConverter;
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
        SolrRequest pubDocsForAccessionSolrRequest =
                publicationRequestConverter.createSearchSolrRequest(request);

        QueryResult<PublicationDocument> results =
                publicationRepository.searchPage(
                        pubDocsForAccessionSolrRequest, request.getCursor());
        List<PublicationDocument> content = results.getContent().collect(Collectors.toList());

        Map<String, LiteratureEntry> pubmedLiteratureEntryMap =
                getPubMedLiteratureEntryMap(content);

        Stream<PublicationEntry> converted =
                content.stream()
                        .map(e -> publicationConverter.apply(e, pubmedLiteratureEntryMap))
                        .filter(Objects::nonNull);
        Set<ProblemPair> warnings =
                getWarnings(
                        request.getQuery(),
                        literatureQueryProcessorConfig.getLeadingWildcardFields());
        return QueryResult.<PublicationEntry>builder()
                .content(converted)
                .page(results.getPage())
                .facets(results.getFacets())
                .warnings(warnings)
                .build();
    }

    public QueryResult<PublicationEntry> getPublicationsByUniProtAccession(
            final String accession, PublicationRequest request) {
        String solrQueryString = accessionFieldName + ":" + accession;
        if (request.getFacetFilter() != null) {
            solrQueryString += " AND (" + request.getFacetFilter() + ")";
        }

        PublicationSearchRequest searchRequest =
                PublicationSearchRequest.builder()
                        .query(solrQueryString)
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

    private Map<String, LiteratureEntry> getPubMedLiteratureEntryMap(
            List<PublicationDocument> content) {
        Map<String, LiteratureEntry> result = new HashMap<>();
        if (!content.isEmpty()) {
            BooleanQuery.Builder pubmedIdsQuery = new BooleanQuery.Builder();
            content.stream()
                    .map(PublicationDocument::getCitationId)
                    .forEach(
                            pubmedId ->
                                    pubmedIdsQuery.add(
                                            new TermQuery(new Term(idFieldName, pubmedId)),
                                            BooleanClause.Occur.SHOULD));
            SolrRequest pubmedIdsSolrRequest = getSolrRequest(pubmedIdsQuery.build().toString());
            Stream<LiteratureDocument> all = literatureRepository.getAll(pubmedIdsSolrRequest);
            result =
                    all.map(literatureEntryConverter)
                            .collect(
                                    Collectors.toMap(
                                            this::getCitationIdFromEntry,
                                            literatureEntry -> literatureEntry));
        }
        return result;
    }

    private String getCitationIdFromEntry(LiteratureEntry entry) {
        return entry.getCitation().getId();
    }

    private SolrRequest getSolrRequest(String query) {
        return SolrRequest.builder()
                .query(query)
                .queryField(literatureSolrQueryConf.getQueryFields())
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
        private String format;

        @Override
        public List<String> getFacetList() {
            if (Utils.notNullNotEmpty(facets)) {
                return Arrays.asList(facets.replaceAll("\\s", "").split(","));
            } else {
                return Collections.emptyList();
            }
        }
    }
}
