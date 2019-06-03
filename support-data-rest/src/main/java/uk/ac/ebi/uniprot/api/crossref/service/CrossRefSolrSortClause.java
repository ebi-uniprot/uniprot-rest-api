package uk.ac.ebi.uniprot.api.crossref.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import uk.ac.ebi.uniprot.api.crossref.model.CrossRefAllFields;
import uk.ac.ebi.uniprot.api.rest.search.AbstractSolrSortClause;

@Service
public class CrossRefSolrSortClause extends AbstractSolrSortClause {

    @Override
    protected Sort createDefaultSort(boolean hasScore) {
        Sort defaultSort = new Sort(Sort.Direction.ASC, CrossRefAllFields.ACCESSION.getSolrFieldName());

        if (hasScore) {
            defaultSort = new Sort(Sort.Direction.DESC, "score").and(defaultSort);
        }

        return defaultSort;
    }

    @Override
    protected String getSolrDocumentIdFieldName() {
        return CrossRefAllFields.ACCESSION.getSolrFieldName();
    }

    @Override
    protected String getSolrSortFieldName(String name) {
        return name;
    }

}