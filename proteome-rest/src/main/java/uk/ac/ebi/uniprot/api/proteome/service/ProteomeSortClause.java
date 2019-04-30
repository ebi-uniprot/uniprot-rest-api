package uk.ac.ebi.uniprot.api.proteome.service;

import org.springframework.data.domain.Sort;

import uk.ac.ebi.uniprot.api.rest.search.AbstractSolrSortClause;
import uk.ac.ebi.uniprot.search.field.ProteomeField;

/**
 *
 * @author jluo
 * @date: 29 Apr 2019
 *
*/

public class ProteomeSortClause extends AbstractSolrSortClause {

	@Override
	protected Sort createDefaultSort(boolean hasScore) {
		 Sort sort = new Sort(Sort.Direction.DESC, ProteomeField.Sort.annotation_score.getSolrFieldName());
	        if(hasScore){
	            sort =sort.and( new Sort(Sort.Direction.DESC, "score"));
	        }
	        return sort;
	}

}

