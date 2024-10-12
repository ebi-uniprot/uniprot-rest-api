package org.uniprot.api.uniparc.common.service.light;

import static org.uniprot.store.search.field.validator.FieldRegexConstants.UNIPARC_UPI_REGEX;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.document.TupleStreamDocumentIdStream;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.common.repository.stream.store.uniparc.UniParcCrossReferenceLazyLoader;
import org.uniprot.api.rest.request.BasicRequest;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.respository.facet.impl.UniParcFacetConfig;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.api.rest.service.StoreStreamerSearchService;
import org.uniprot.api.rest.service.query.config.UniParcSolrQueryConfig;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
import org.uniprot.api.uniparc.common.repository.search.UniParcQueryRepository;
import org.uniprot.api.uniparc.common.response.converter.UniParcLightQueryResultConverter;
import org.uniprot.api.uniparc.common.service.request.UniParcBasicRequest;
import org.uniprot.api.uniparc.common.service.request.UniParcGetByIdPageSearchRequest;
import org.uniprot.api.uniparc.common.service.request.UniParcStreamRequest;
import org.uniprot.api.uniparc.common.service.sort.UniParcSortClause;
import org.uniprot.core.uniparc.UniParcEntryLight;
import org.uniprot.core.uniparc.impl.UniParcEntryLightBuilder;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.search.document.uniparc.UniParcDocument;

@Service
@Import(UniParcSolrQueryConfig.class)
public class UniParcLightEntryService
        extends StoreStreamerSearchService<UniParcDocument, UniParcEntryLight> {

    public static final String UNIPARC_ID_FIELD = "upi";

    private static final Pattern UNIPARC_UPI_REGEX_PATTERN = Pattern.compile(UNIPARC_UPI_REGEX);
    private final UniProtQueryProcessorConfig uniParcQueryProcessorConfig;
    private final SearchFieldConfig searchFieldConfig;
    private final UniParcCrossReferenceLazyLoader uniParcCrossReferenceLazyLoader;

    private final SolrQueryConfig solrQueryConfig;
    private final RdfStreamer rdfStreamer;

    @Autowired
    public UniParcLightEntryService(
            UniParcQueryRepository repository,
            UniParcFacetConfig facetConfig,
            UniParcSortClause solrSortClause,
            UniParcLightQueryResultConverter uniParcLightQueryResultConverter,
            StoreStreamer<UniParcEntryLight> storeStreamer,
            SolrQueryConfig uniParcSolrQueryConf,
            UniProtQueryProcessorConfig uniParcQueryProcessorConfig,
            SearchFieldConfig uniParcSearchFieldConfig,
            RdfStreamer uniParcRdfStreamer,
            FacetTupleStreamTemplate uniParcFacetTupleStreamTemplate,
            TupleStreamDocumentIdStream uniParcTupleStreamDocumentIdStream,
            UniParcCrossReferenceLazyLoader uniParcCrossReferenceLazyLoader) {

        super(
                repository,
                uniParcLightQueryResultConverter,
                solrSortClause,
                facetConfig,
                storeStreamer,
                uniParcSolrQueryConf,
                uniParcFacetTupleStreamTemplate,
                uniParcTupleStreamDocumentIdStream);
        this.uniParcQueryProcessorConfig = uniParcQueryProcessorConfig;
        this.searchFieldConfig = uniParcSearchFieldConfig;
        this.uniParcCrossReferenceLazyLoader = uniParcCrossReferenceLazyLoader;
        this.solrQueryConfig = uniParcSolrQueryConf;
        this.rdfStreamer = uniParcRdfStreamer;
    }

    @Override
    public UniParcEntryLight findByUniqueId(String uniqueId, String filters) {
        UniParcEntryLight entryLight = findByUniqueId(uniqueId);
        List<String> lazyFields = uniParcCrossReferenceLazyLoader.getLazyFields(filters);
        if (Utils.notNullNotEmpty(lazyFields)) {
            entryLight = uniParcCrossReferenceLazyLoader.populateLazyFields(entryLight, lazyFields);
        }
        return entryLight;
    }

    public Stream<String> streamRdf(
            UniParcStreamRequest streamRequest, String dataType, String format) {
        SolrRequest solrRequest =
                createSolrRequestBuilder(streamRequest, solrSortClause, solrQueryConfig).build();
        List<String> entryIds = solrIdStreamer.fetchIds(solrRequest).toList();
        return rdfStreamer.stream(entryIds.stream(), dataType, format);
    }

    public QueryResult<UniParcEntryLight> searchByFieldId(
            UniParcGetByIdPageSearchRequest searchRequest) {
        return super.search(searchRequest);
    }

    @Override
    protected SearchFieldItem getIdField() {
        return this.searchFieldConfig.getSearchFieldItemByName(UNIPARC_ID_FIELD);
    }

    @Override
    protected UniParcEntryLight mapToThinEntry(String entryId) {
        UniParcEntryLightBuilder builder = new UniParcEntryLightBuilder();
        builder.uniParcId(entryId);
        return builder.build();
    }

    @Override
    protected UniProtQueryProcessorConfig getQueryProcessorConfig() {
        return uniParcQueryProcessorConfig;
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.UNIPARC;
    }

    @Override
    protected String getSolrIdField() {
        return SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIPARC)
                .getSearchFieldItemByName(UNIPARC_ID_FIELD)
                .getFieldName();
    }

    @Override
    protected Stream<UniParcEntryLight> convertDocumentsToEntries(
            SearchRequest request, QueryResult<UniParcDocument> results) {
        Stream<UniParcEntryLight> result = super.convertDocumentsToEntries(request, results);
        List<String> lazyFields =
                uniParcCrossReferenceLazyLoader.getLazyFields(request.getFields());
        if (Utils.notNullNotEmpty(lazyFields)) {
            result =
                    result.map(
                            entry ->
                                    uniParcCrossReferenceLazyLoader.populateLazyFields(
                                            entry, lazyFields));
        }
        return result;
    }

    @Override
    protected SolrRequest.SolrRequestBuilder createSolrRequestBuilder(
            BasicRequest request,
            AbstractSolrSortClause solrSortClause,
            SolrQueryConfig queryBoosts) {
        if (request instanceof UniParcBasicRequest uniParcBasicRequest) {
            String cleanQuery =
                    CLEAN_QUERY_REGEX.matcher(request.getQuery().strip()).replaceAll("");
            if (UNIPARC_UPI_REGEX_PATTERN.matcher(cleanQuery.toUpperCase()).matches()) {
                uniParcBasicRequest.setQuery(cleanQuery.toUpperCase());
            }
            return super.createSolrRequestBuilder(uniParcBasicRequest, solrSortClause, queryBoosts);
        }
        return super.createSolrRequestBuilder(request, solrSortClause, queryBoosts);
    }

    @Override
    protected RdfStreamer getRdfStreamer() {
        return this.rdfStreamer;
    }
}
