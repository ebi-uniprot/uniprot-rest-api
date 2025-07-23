package org.uniprot.api.proteome.service;

import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.stream.document.DefaultDocumentIdStream;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;
import org.uniprot.api.proteome.repository.ProteomeQueryRepository;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.rest.service.request.RequestConverter;
import org.uniprot.core.proteome.ProteomeEntry;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.search.document.proteome.ProteomeDocument;

/**
 * @author jluo
 * @date: 26 Apr 2019
 */
@Service
@Import(ProteomeSolrQueryConfig.class)
public class ProteomeQueryService extends BasicSearchService<ProteomeDocument, ProteomeEntry> {
    public static final String PROTEOME_ID_FIELD = "upid";
    private final SearchFieldConfig fieldConfig;
    private final RdfStreamer rdfStreamer;
    private final DefaultDocumentIdStream<ProteomeDocument> documentIdStream;

    public ProteomeQueryService(
            ProteomeQueryRepository repository,
            SearchFieldConfig proteomeSearchFieldConfig,
            RdfStreamer proteomeRdfStreamer,
            RequestConverter proteomeRequestConverter,
            DefaultDocumentIdStream<ProteomeDocument> documentIdStream) {
        super(repository, new ProteomeEntryConverter(), proteomeRequestConverter);
        this.fieldConfig = proteomeSearchFieldConfig;
        this.rdfStreamer = proteomeRdfStreamer;
        this.documentIdStream = documentIdStream;
    }

    @Override
    protected SearchFieldItem getIdField() {
        return fieldConfig.getSearchFieldItemByName(PROTEOME_ID_FIELD);
    }

    @Override
    protected RdfStreamer getRdfStreamer() {
        return rdfStreamer;
    }

    @Override
    protected DefaultDocumentIdStream<ProteomeDocument> getDocumentIdStream() {
        return documentIdStream;
    }
}
