package uk.ac.ebi.uniprot.api.disease;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import uk.ac.ebi.uniprot.api.crossref.model.CrossRefAllFields;
import uk.ac.ebi.uniprot.api.disease.validator.DiseaseFields;
import uk.ac.ebi.uniprot.api.rest.search.AbstractSolrSortClause;

@Service
public class DiseaseSolrSortClause extends AbstractSolrSortClause {

    @Override
    protected Sort createDefaultSort(boolean hasScore) {
        Sort defaultSort = new Sort(Sort.Direction.ASC, DiseaseFields.ACCESSION.getSolrFieldName());

        if (hasScore) {
            defaultSort = new Sort(Sort.Direction.DESC, "score").and(defaultSort);
        }

        return defaultSort;
    }
}