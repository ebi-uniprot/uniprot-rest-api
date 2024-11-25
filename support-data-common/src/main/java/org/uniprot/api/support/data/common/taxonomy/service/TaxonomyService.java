package org.uniprot.api.support.data.common.taxonomy.service;

import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.stream.document.DefaultDocumentIdStream;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.rest.service.request.RequestConverter;
import org.uniprot.api.support.data.common.taxonomy.repository.TaxonomyRepository;
import org.uniprot.core.taxonomy.TaxonomyEntry;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.search.document.taxonomy.TaxonomyDocument;

@Service
@Import(TaxonomySolrQueryConfig.class)
public class TaxonomyService extends BasicSearchService<TaxonomyDocument, TaxonomyEntry> {
    public static final String TAXONOMY_ID_FIELD = "id";
    private final SearchFieldConfig searchFieldConfig;
    private final RdfStreamer rdfStreamer;
    private final DefaultDocumentIdStream<TaxonomyDocument> documentIdStream;

    public TaxonomyService(
            TaxonomyRepository repository,
            TaxonomyEntryConverter converter,
            SearchFieldConfig taxonomySearchFieldConfig,
            RdfStreamer supportDataRdfStreamer,
            DefaultDocumentIdStream<TaxonomyDocument> documentIdStream,
            RequestConverter taxonomyRequestConverter) {

        super(repository, converter, taxonomyRequestConverter);
        this.searchFieldConfig = taxonomySearchFieldConfig;
        this.rdfStreamer = supportDataRdfStreamer;
        this.documentIdStream = documentIdStream;
    }

    public TaxonomyEntry findById(final long taxId) {
        return findByUniqueId(String.valueOf(taxId));
    }

    @Override
    protected SearchFieldItem getIdField() {
        return this.searchFieldConfig.getSearchFieldItemByName(TAXONOMY_ID_FIELD);
    }

    @Override
    protected RdfStreamer getRdfStreamer() {
        return this.rdfStreamer;
    }

    @Override
    protected DefaultDocumentIdStream<TaxonomyDocument> getDocumentIdStream() {
        return this.documentIdStream;
    }
}
