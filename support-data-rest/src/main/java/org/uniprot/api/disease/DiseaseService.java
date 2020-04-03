package org.uniprot.api.disease;

import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.QueryBoosts;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.core.cv.disease.DiseaseEntry;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.search.document.disease.DiseaseDocument;

@Service
@Import(DiseaseQueryBoostsConfig.class)
public class DiseaseService extends BasicSearchService<DiseaseDocument, DiseaseEntry> {
    private SearchFieldConfig searchFieldConfig;

    public DiseaseService(
            DiseaseRepository diseaseRepository,
            DiseaseDocumentToDiseaseConverter toDiseaseConverter,
            DiseaseSolrSortClause diseaseSolrSortClause,
            QueryBoosts diseaseQueryBoosts) {

        super(
                diseaseRepository,
                toDiseaseConverter,
                diseaseSolrSortClause,
                diseaseQueryBoosts,
                null);
        this.searchFieldConfig =
                SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.DISEASE);
    }

    @Override
    protected String getIdField() {
        return searchFieldConfig.getSearchFieldItemByName("id").getFieldName();
    }
}
