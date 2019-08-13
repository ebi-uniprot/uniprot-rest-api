package org.uniprot.api.proteome.service;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.search.field.ProteomeField;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author jluo
 * @date: 29 Apr 2019
 *
*/
@Component
public class ProteomeSortClause extends AbstractSolrSortClause {

	@Override
	protected Sort createDefaultSort(boolean hasScore) {
		  return new Sort(Sort.Direction.DESC, ProteomeField.Sort.annotation_score.getSolrFieldName())
	                .and(new Sort(Sort.Direction.ASC, ProteomeField.Sort.upid.getSolrFieldName()));
	}

	@Override
	protected String getSolrDocumentIdFieldName() {
		return ProteomeField.Search.upid.name();
	}

	@Override
	protected String getSolrSortFieldName(String name) {
		return name;
	}

	@Override
	  protected List<Pair<String, Sort.Direction>> parseSortClause(String sortClause) {
		List<Pair<String, Sort.Direction>> fieldSortPairs =super.parseSortClause(sortClause);
		if (fieldSortPairs.stream().anyMatch(val -> val.getLeft().equals(ProteomeField.Sort.upid.getSolrFieldName()))) {
			return fieldSortPairs;
		}else {
			List<Pair<String, Sort.Direction>> newFieldSortPairs =new ArrayList<>();
			newFieldSortPairs.addAll(fieldSortPairs);
			newFieldSortPairs.add(new ImmutablePair<>(ProteomeField.Sort.upid.getSolrFieldName(),Sort.Direction.ASC ));
			return newFieldSortPairs;
		}
	}
}

