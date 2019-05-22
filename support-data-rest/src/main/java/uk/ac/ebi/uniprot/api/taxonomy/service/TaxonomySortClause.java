package uk.ac.ebi.uniprot.api.taxonomy.service;

import org.springframework.data.domain.Sort;
import uk.ac.ebi.uniprot.api.rest.search.AbstractSolrSortClause;
import uk.ac.ebi.uniprot.search.field.TaxonomyField;

public class TaxonomySortClause extends AbstractSolrSortClause {

    @Override
    protected Sort createDefaultSort(boolean hasScore) {
        return new Sort(Sort.Direction.DESC, "score")
                .and(new Sort(Sort.Direction.ASC, TaxonomyField.Search.tax_id.name()))
                .and(new Sort(Sort.Direction.ASC, TaxonomyField.Search.id.name()));
    }

}
