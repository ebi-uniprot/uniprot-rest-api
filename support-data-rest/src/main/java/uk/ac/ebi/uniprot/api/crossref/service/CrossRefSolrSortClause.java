package uk.ac.ebi.uniprot.api.crossref.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import uk.ac.ebi.uniprot.api.rest.search.AbstractSolrSortClause;
import uk.ac.ebi.uniprot.search.field.CrossRefField;

@Service
public class CrossRefSolrSortClause extends AbstractSolrSortClause {

    @Override
    protected Sort createDefaultSort(boolean hasScore) {
        Sort defaultSort = new Sort(Sort.Direction.ASC, CrossRefField.Search.accession.getName());

        if (hasScore) {
            defaultSort = new Sort(Sort.Direction.DESC, "score").and(defaultSort);
        }

        return defaultSort;
    }

    @Override
    protected String getSolrDocumentIdFieldName() {
        return CrossRefField.Search.accession.getName();
    }

    @Override
    protected String getSolrSortFieldName(String name) {
        return name;
    }

}