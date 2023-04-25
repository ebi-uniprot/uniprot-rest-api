package org.uniprot.api.support.data.disease.service;

import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.stream.document.DefaultDocumentIdStream;
import org.uniprot.api.common.repository.stream.rdf.RDFStreamer;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
import org.uniprot.api.support.data.disease.repository.DiseaseRepository;
import org.uniprot.api.support.data.disease.request.DiseaseSolrQueryConfig;
import org.uniprot.api.support.data.disease.request.DiseaseSolrSortClause;
import org.uniprot.api.support.data.disease.response.DiseaseDocumentToDiseaseConverter;
import org.uniprot.core.cv.disease.DiseaseEntry;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.search.document.disease.DiseaseDocument;

@Service
@Import(DiseaseSolrQueryConfig.class)
public class DiseaseService extends BasicSearchService<DiseaseDocument, DiseaseEntry> {
    public static final String DISEASE_ID_FIELD = "id";
    private final UniProtQueryProcessorConfig diseaseQueryProcessorConfig;
    private final SearchFieldConfig searchFieldConfig;
    private final RDFStreamer rdfStreamer;
    private final DefaultDocumentIdStream<DiseaseDocument> documentIdStream;

    public DiseaseService(
            DiseaseRepository diseaseRepository,
            DiseaseDocumentToDiseaseConverter toDiseaseConverter,
            DiseaseSolrSortClause diseaseSolrSortClause,
            SolrQueryConfig diseaseSolrQueryConf,
            UniProtQueryProcessorConfig diseaseQueryProcessorConfig,
            SearchFieldConfig diseaseSearchFieldConfig,
            DefaultDocumentIdStream<DiseaseDocument> documentIdStream,
            RDFStreamer supportDataRdfStreamer) {

        super(
                diseaseRepository,
                toDiseaseConverter,
                diseaseSolrSortClause,
                diseaseSolrQueryConf,
                null);
        this.diseaseQueryProcessorConfig = diseaseQueryProcessorConfig;
        this.searchFieldConfig = diseaseSearchFieldConfig;
        this.rdfStreamer = supportDataRdfStreamer;
        this.documentIdStream = documentIdStream;
    }

    @Override
    protected SearchFieldItem getIdField() {
        return searchFieldConfig.getSearchFieldItemByName(DISEASE_ID_FIELD);
    }

    @Override
    protected UniProtQueryProcessorConfig getQueryProcessorConfig() {
        return diseaseQueryProcessorConfig;
    }

    @Override
    protected DefaultDocumentIdStream<DiseaseDocument> getDocumentIdStream() {
        return documentIdStream;
    }

    @Override
    protected RDFStreamer getRDFStreamer() {
        return rdfStreamer;
    }
}
