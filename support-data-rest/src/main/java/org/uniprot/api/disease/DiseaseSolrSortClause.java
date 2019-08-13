package org.uniprot.api.disease;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.search.field.DiseaseField;

@Service
public class DiseaseSolrSortClause extends AbstractSolrSortClause {

    @Override
    protected Sort createDefaultSort(boolean hasScore) {
        Sort defaultSort = new Sort(Sort.Direction.ASC, DiseaseField.Sort.accession.getSolrFieldName());

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