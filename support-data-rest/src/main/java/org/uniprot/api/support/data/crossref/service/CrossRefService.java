package org.uniprot.api.support.data.crossref.service;

import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.stream.document.DefaultDocumentIdStream;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.rest.service.request.RequestConverter;
import org.uniprot.api.support.data.crossref.repository.CrossRefRepository;
import org.uniprot.api.support.data.crossref.request.*;
import org.uniprot.core.cv.xdb.CrossRefEntry;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.search.document.dbxref.CrossRefDocument;

@Service
@Import(CrossRefSolrQueryConfig.class)
public class CrossRefService extends BasicSearchService<CrossRefDocument, CrossRefEntry> {
    public static final String CROSS_REF_ID_FIELD = "id";
    private final SearchFieldConfig searchFieldConfig;
    private final RdfStreamer rdfStreamer;
    private final DefaultDocumentIdStream<CrossRefDocument> documentIdStream;

    public CrossRefService(
            CrossRefRepository crossRefRepository,
            CrossRefEntryConverter toCrossRefEntryConverter,
            SearchFieldConfig crossRefSearchFieldConfig,
            DefaultDocumentIdStream<CrossRefDocument> documentIdStream,
            RdfStreamer supportDataRdfStreamer,
            RequestConverter crossRefRequestConverter) {
        super(crossRefRepository, toCrossRefEntryConverter, crossRefRequestConverter);
        this.searchFieldConfig = crossRefSearchFieldConfig;
        this.documentIdStream = documentIdStream;
        this.rdfStreamer = supportDataRdfStreamer;
    }

    @Override
    protected SearchFieldItem getIdField() {
        return searchFieldConfig.getSearchFieldItemByName(CROSS_REF_ID_FIELD);
    }

    @Override
    protected DefaultDocumentIdStream<CrossRefDocument> getDocumentIdStream() {
        return documentIdStream;
    }

    @Override
    protected RdfStreamer getRdfStreamer() {
        return this.rdfStreamer;
    }
}
