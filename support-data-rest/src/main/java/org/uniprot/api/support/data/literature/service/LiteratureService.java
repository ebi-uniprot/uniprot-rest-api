package org.uniprot.api.support.data.literature.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.stream.rdf.RDFStreamer;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.rest.service.query.QueryProcessor;
import org.uniprot.api.rest.service.query.config.LiteratureSolrQueryConfig;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
import org.uniprot.api.support.data.literature.repository.LiteratureFacetConfig;
import org.uniprot.api.support.data.literature.repository.LiteratureRepository;
import org.uniprot.api.support.data.literature.request.LiteratureSortClause;
import org.uniprot.api.support.data.literature.response.LiteratureEntryConverter;
import org.uniprot.core.literature.LiteratureEntry;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.search.document.literature.LiteratureDocument;

/**
 * @author lgonzales
 * @since 2019-07-04
 */
@Service
@Import(LiteratureSolrQueryConfig.class)
public class LiteratureService extends BasicSearchService<LiteratureDocument, LiteratureEntry> {
    public static final String LITERATURE_ID_FIELD = "id";
    private final UniProtQueryProcessorConfig literatureQueryProcessorConfig;
    private final SearchFieldConfig searchFieldConfig;
    private final RDFStreamer rdfStreamer;

    public LiteratureService(
            LiteratureRepository repository,
            LiteratureEntryConverter entryConverter,
            LiteratureFacetConfig facetConfig,
            LiteratureSortClause literatureSortClause,
            SolrQueryConfig literatureSolrQueryConf,
            UniProtQueryProcessorConfig literatureQueryProcessorConfig,
            SearchFieldConfig literatureSearchFieldConfig,
            @Qualifier("literatureRDFStreamer") RDFStreamer rdfStreamer) {
        super(
                repository,
                entryConverter,
                literatureSortClause,
                literatureSolrQueryConf,
                facetConfig);
        this.literatureQueryProcessorConfig = literatureQueryProcessorConfig;
        this.searchFieldConfig = literatureSearchFieldConfig;
        this.rdfStreamer = rdfStreamer;
    }

    @Override
    protected SearchFieldItem getIdField() {
        return this.searchFieldConfig.getSearchFieldItemByName(LITERATURE_ID_FIELD);
    }

    @Override
    protected UniProtQueryProcessorConfig getQueryProcessorConfig() {
        return literatureQueryProcessorConfig;
    }

    @Override
    protected RDFStreamer getRDFStreamer() {
        return this.rdfStreamer;
    }
}
