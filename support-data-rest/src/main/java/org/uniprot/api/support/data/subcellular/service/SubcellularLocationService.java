package org.uniprot.api.support.data.subcellular.service;

import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.stream.document.DefaultDocumentIdStream;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.rest.service.request.RequestConverter;
import org.uniprot.api.support.data.subcellular.repository.SubcellularLocationRepository;
import org.uniprot.api.support.data.subcellular.request.SubcellularLocationSolrQueryConfig;
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
    private final SearchFieldConfig searchFieldConfig;
    private final RdfStreamer rdfStreamer;
    private final DefaultDocumentIdStream<SubcellularLocationDocument> documentIdStream;

    public SubcellularLocationService(
            SubcellularLocationRepository repository,
            SubcellularLocationEntryConverter subcellularLocationEntryConverter,
            SearchFieldConfig subcellSearchFieldConfig,
            DefaultDocumentIdStream<SubcellularLocationDocument> documentIdStream,
            RdfStreamer supportDataRdfStreamer,
            RequestConverter subcellRequestConverter) {
        super(repository, subcellularLocationEntryConverter, subcellRequestConverter);
        this.searchFieldConfig = subcellSearchFieldConfig;
        this.documentIdStream = documentIdStream;
        this.rdfStreamer = supportDataRdfStreamer;
    }

    @Override
    protected SearchFieldItem getIdField() {
        return this.searchFieldConfig.getSearchFieldItemByName(SUBCELL_ID_FIELD);
    }

    @Override
    protected RdfStreamer getRdfStreamer() {
        return this.rdfStreamer;
    }

    @Override
    protected DefaultDocumentIdStream<SubcellularLocationDocument> getDocumentIdStream() {
        return this.documentIdStream;
    }
}
