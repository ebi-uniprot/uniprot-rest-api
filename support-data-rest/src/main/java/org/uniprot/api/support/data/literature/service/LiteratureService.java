package org.uniprot.api.support.data.literature.service;

import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.stream.document.DefaultDocumentIdStream;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.rest.service.query.config.LiteratureSolrQueryConfig;
import org.uniprot.api.rest.service.request.RequestConverter;
import org.uniprot.api.support.data.literature.repository.LiteratureRepository;
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
    private final SearchFieldConfig searchFieldConfig;
    private final RdfStreamer rdfStreamer;
    private final DefaultDocumentIdStream<LiteratureDocument> documentIdStream;

    public LiteratureService(
            LiteratureRepository repository,
            LiteratureEntryConverter entryConverter,
            SearchFieldConfig literatureSearchFieldConfig,
            DefaultDocumentIdStream<LiteratureDocument> documentIdStream,
            RdfStreamer supportDataRdfStreamer,
            RequestConverter literatureRequestConverter) {
        super(repository, entryConverter, literatureRequestConverter);
        this.searchFieldConfig = literatureSearchFieldConfig;
        this.rdfStreamer = supportDataRdfStreamer;
        this.documentIdStream = documentIdStream;
    }

    @Override
    protected SearchFieldItem getIdField() {
        return this.searchFieldConfig.getSearchFieldItemByName(LITERATURE_ID_FIELD);
    }

    @Override
    protected RdfStreamer getRdfStreamer() {
        return this.rdfStreamer;
    }

    @Override
    protected DefaultDocumentIdStream<LiteratureDocument> getDocumentIdStream() {
        return this.documentIdStream;
    }
}
