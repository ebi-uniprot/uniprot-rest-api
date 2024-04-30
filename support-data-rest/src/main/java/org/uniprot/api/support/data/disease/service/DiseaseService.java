package org.uniprot.api.support.data.disease.service;

import static org.uniprot.store.search.field.validator.FieldRegexConstants.DISEASE_REGEX;

import java.util.regex.Pattern;

import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.stream.document.DefaultDocumentIdStream;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
import org.uniprot.api.support.data.disease.repository.DiseaseRepository;
import org.uniprot.api.support.data.disease.request.DiseaseSearchRequest;
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
    private final RdfStreamer rdfStreamer;
    private final DefaultDocumentIdStream<DiseaseDocument> documentIdStream;
    private static final Pattern DISEASE_REGEX_PATTERN = Pattern.compile(DISEASE_REGEX);

    public DiseaseService(
            DiseaseRepository diseaseRepository,
            DiseaseDocumentToDiseaseConverter toDiseaseConverter,
            DiseaseSolrSortClause diseaseSolrSortClause,
            SolrQueryConfig diseaseSolrQueryConf,
            UniProtQueryProcessorConfig diseaseQueryProcessorConfig,
            SearchFieldConfig diseaseSearchFieldConfig,
            DefaultDocumentIdStream<DiseaseDocument> documentIdStream,
            RdfStreamer supportDataRdfStreamer) {

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
    protected RdfStreamer getRdfStreamer() {
        return rdfStreamer;
    }

    @Override
    public SolrRequest createSearchSolrRequest(SearchRequest request) {
        DiseaseSearchRequest searchRequest = (DiseaseSearchRequest) request;
        String cleanQuery = CLEAN_QUERY_REGEX.matcher(request.getQuery().strip()).replaceAll("");
        if (DISEASE_REGEX_PATTERN.matcher(cleanQuery.toUpperCase()).matches()) {
            searchRequest.setQuery(cleanQuery.toUpperCase());
        }
        return super.createSearchSolrRequest(searchRequest);
    }
}
