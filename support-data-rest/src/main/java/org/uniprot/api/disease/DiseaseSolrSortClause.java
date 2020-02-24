package org.uniprot.api.disease;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.factory.UniProtDataType;
import org.uniprot.store.search.field.DiseaseField;

@Component
public class DiseaseSolrSortClause extends AbstractSolrSortClause {
    private SearchFieldConfig searchFieldConfig =
            SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.disease);

    @Override
    protected Sort createDefaultSort(boolean hasScore) {
        Sort defaultSort =
                new Sort(
                        Sort.Direction.ASC,
                        searchFieldConfig.getCorrespondingSortField("accession").getFieldName());

        if (hasScore) {
            defaultSort = new Sort(Sort.Direction.DESC, "score").and(defaultSort);
        }

        return defaultSort;
    }

    @Override
    protected String getSolrDocumentIdFieldName() {
        return DiseaseField.ResultFields.accession.name();
    }

    @Override
    protected String getSolrSortFieldName(String name) {
        return name;
    }
}
