package org.uniprot.api.support.data.subcellular.service;

import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.stream.document.DefaultDocumentIdStream;
import org.uniprot.api.common.repository.stream.rdf.RDFStreamer;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
import org.uniprot.api.support.data.subcellular.repository.SubcellularLocationRepository;
import org.uniprot.api.support.data.subcellular.request.SubcellularLocationSolrQueryConfig;
import org.uniprot.api.support.data.subcellular.request.SubcellularLocationSortClause;
import org.uniprot.api.support.data.subcellular.response.SubcellularLocationEntryConverter;
import org.uniprot.core.cv.subcell.SubcellularLocationEntry;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.search.document.subcell.SubcellularLocationDocument;

/**
 * @author lgonzales
 * @since 2019-07-19
 */
@Service
@Import(SubcellularLocationSolrQueryConfig.class)
public class SubcellularLocationService
        extends BasicSearchService<SubcellularLocationDocument, SubcellularLocationEntry> {
    public static final String SUBCELL_ID_FIELD = "id";
    private final UniProtQueryProcessorConfig subcellQueryProcessorConfig;
    private final SearchFieldConfig searchFieldConfig;
    private final RDFStreamer rdfStreamer;
    private final DefaultDocumentIdStream<SubcellularLocationDocument> documentIdStream;

    public SubcellularLocationService(
            SubcellularLocationRepository repository,
            SubcellularLocationEntryConverter subcellularLocationEntryConverter,
            SubcellularLocationSortClause subcellularLocationSortClause,
            SolrQueryConfig subcellSolrQueryConf,
            UniProtQueryProcessorConfig subcellQueryProcessorConfig,
            SearchFieldConfig subcellSearchFieldConfig,
            DefaultDocumentIdStream<SubcellularLocationDocument> documentIdStream,
            RDFStreamer supportDataRdfXmlStreamer) {
        super(
                repository,
                subcellularLocationEntryConverter,
                subcellularLocationSortClause,
                subcellSolrQueryConf,
                null);
        this.subcellQueryProcessorConfig = subcellQueryProcessorConfig;
        this.searchFieldConfig = subcellSearchFieldConfig;
        this.documentIdStream = documentIdStream;
        this.rdfStreamer = supportDataRdfXmlStreamer;
    }

    @Override
    protected SearchFieldItem getIdField() {
        return this.searchFieldConfig.getSearchFieldItemByName(SUBCELL_ID_FIELD);
    }

    @Override
    protected UniProtQueryProcessorConfig getQueryProcessorConfig() {
        return subcellQueryProcessorConfig;
    }

    @Override
    protected RDFStreamer getRDFStreamer() {
        return this.rdfStreamer;
    }

    @Override
    protected DefaultDocumentIdStream<SubcellularLocationDocument> getDocumentIdStream() {
        return this.documentIdStream    ;
    }
}
