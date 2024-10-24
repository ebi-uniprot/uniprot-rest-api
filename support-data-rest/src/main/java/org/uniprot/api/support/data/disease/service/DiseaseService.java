package org.uniprot.api.support.data.disease.service;

import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.stream.document.DefaultDocumentIdStream;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.rest.service.request.RequestConverter;
import org.uniprot.api.support.data.disease.repository.DiseaseRepository;
import org.uniprot.api.support.data.disease.request.DiseaseSolrQueryConfig;
import org.uniprot.api.support.data.disease.response.DiseaseDocumentToDiseaseConverter;
import org.uniprot.core.cv.disease.DiseaseEntry;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.search.document.disease.DiseaseDocument;

@Service
@Import(DiseaseSolrQueryConfig.class)
public class DiseaseService extends BasicSearchService<DiseaseDocument, DiseaseEntry> {
    public static final String DISEASE_ID_FIELD = "id";
    private final SearchFieldConfig searchFieldConfig;
    private final RdfStreamer rdfStreamer;
    private final DefaultDocumentIdStream<DiseaseDocument> documentIdStream;

    public DiseaseService(
            DiseaseRepository diseaseRepository,
            DiseaseDocumentToDiseaseConverter toDiseaseConverter,
            SearchFieldConfig diseaseSearchFieldConfig,
            DefaultDocumentIdStream<DiseaseDocument> documentIdStream,
            RdfStreamer supportDataRdfStreamer,
            RequestConverter diseaseRequestConverter) {

        super(diseaseRepository, toDiseaseConverter, diseaseRequestConverter);
        this.searchFieldConfig = diseaseSearchFieldConfig;
        this.rdfStreamer = supportDataRdfStreamer;
        this.documentIdStream = documentIdStream;
    }

    @Override
    protected SearchFieldItem getIdField() {
        return searchFieldConfig.getSearchFieldItemByName(DISEASE_ID_FIELD);
    }

    @Override
    protected DefaultDocumentIdStream<DiseaseDocument> getDocumentIdStream() {
        return documentIdStream;
    }

    @Override
    protected RdfStreamer getRdfStreamer() {
        return rdfStreamer;
    }
}
