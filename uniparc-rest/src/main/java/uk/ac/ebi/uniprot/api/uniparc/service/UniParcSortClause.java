package uk.ac.ebi.uniprot.api.uniparc.service;

import org.springframework.data.domain.Sort;

import uk.ac.ebi.uniprot.api.rest.search.AbstractSolrSortClause;
import uk.ac.ebi.uniprot.search.field.UniParcField;

/**
 *
 * @author jluo
 * @date: 21 Jun 2019
 *
*/

public class UniParcSortClause extends AbstractSolrSortClause {

	@Override
	protected Sort createDefaultSort(boolean hasScore) {
		  return new Sort(Sort.Direction.ASC, UniParcField.Sort.upi.getSolrFieldName());
	}

	@Override
	protected String getSolrDocumentIdFieldName() {
		return UniParcField.Search.upi.name();
	}

	@Override
	protected String getSolrSortFieldName(String name) {
		return name;
	}

}

