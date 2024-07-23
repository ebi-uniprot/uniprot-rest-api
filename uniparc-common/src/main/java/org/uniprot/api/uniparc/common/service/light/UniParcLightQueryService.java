package org.uniprot.api.uniparc.common.service.light;

import java.util.List;
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
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.respository.facet.impl.UniParcFacetConfig;
import org.uniprot.api.rest.service.StoreStreamerSearchService;
import org.uniprot.api.rest.service.query.config.UniParcSolrQueryConfig;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
import org.uniprot.api.uniparc.common.repository.search.UniParcQueryRepository;
import org.uniprot.api.uniparc.common.repository.store.crossref.UniParcCrossReferenceLazyLoader;
import org.uniprot.api.uniparc.common.response.converter.UniParcLightQueryResultConverter;
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
public class UniParcLightQueryService
        extends StoreStreamerSearchService<UniParcDocument, UniParcEntryLight> {

    public static final String UNIPARC_ID_FIELD = "upi";
    private final UniProtQueryProcessorConfig uniParcQueryProcessorConfig;
    private final SearchFieldConfig searchFieldConfig;
    private final UniParcCrossReferenceLazyLoader uniParcCrossReferenceLazyLoader;

    private final SolrQueryConfig solrQueryConfig;
    private final RdfStreamer rdfStreamer;

    @Autowired
    public UniParcLightQueryService(
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
    public UniParcEntryLight findByUniqueId(String uniqueId, String filters) {
        UniParcEntryLight entryLight = findByUniqueId(uniqueId);
        List<String> lazyFields = uniParcCrossReferenceLazyLoader.getLazyFields(filters);
        if (Utils.notNullNotEmpty(lazyFields)) {
            entryLight = uniParcCrossReferenceLazyLoader.loadLazyLoadFields(entryLight, lazyFields);
        }
        return entryLight;
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
                                    uniParcCrossReferenceLazyLoader.loadLazyLoadFields(
                                            entry, lazyFields));
        }
        return result;
    }

    @Override
    protected RdfStreamer getRdfStreamer() {
        return this.rdfStreamer;
    }

    public Stream<String> streamRdf(
            UniParcStreamRequest streamRequest, String dataType, String format) {
        SolrRequest solrRequest =
                createSolrRequestBuilder(streamRequest, solrSortClause, solrQueryConfig).build();
        List<String> entryIds = solrIdStreamer.fetchIds(solrRequest).toList();
        return rdfStreamer.stream(entryIds.stream(), dataType, format);
    }
}
