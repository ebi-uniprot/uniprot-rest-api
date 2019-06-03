package uk.ac.ebi.uniprot.api.proteome.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import uk.ac.ebi.uniprot.api.rest.search.AbstractSolrSortClause;
import uk.ac.ebi.uniprot.search.field.GeneCentricField;

/**
 *
 * @author jluo
 * @date: 17 May 2019
 *
*/
@Component
public class GeneCentricSortClause extends AbstractSolrSortClause {
	@Override
	protected Sort createDefaultSort(boolean hasScore) {
		  return new Sort(Sort.Direction.ASC, GeneCentricField.Sort.accession_id.getSolrFieldName());
	}
	@Override
	  protected List<Pair<String, Sort.Direction>> parseSortClause(String sortClause) {
		List<Pair<String, Sort.Direction>> fieldSortPairs =super.parseSortClause(sortClause);
		if (fieldSortPairs.stream().anyMatch(val -> val.getLeft().equals(GeneCentricField.Sort.accession_id.getSolrFieldName()))) {
			return fieldSortPairs;
		}else {
			List<Pair<String, Sort.Direction>> newFieldSortPairs =new ArrayList<>();
			newFieldSortPairs.addAll(fieldSortPairs);
			newFieldSortPairs.add(new ImmutablePair<>(GeneCentricField.Sort.accession_id.getSolrFieldName(),Sort.Direction.ASC ));
			return newFieldSortPairs;
		}
	}
}

