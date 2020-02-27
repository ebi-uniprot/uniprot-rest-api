package org.uniprot.api.rest.search;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.domain.Sort;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.factory.UniProtDataType;

/**
 * A class to handle solr sort clause, like parsing, providing default sort order
 *
 * @author sahmad
 */
public abstract class AbstractSolrSortClause {
    public static final String SCORE = "score";

    public Sort getSort(String sortClause, boolean hasScore) {
        Sort result;
        if (StringUtils.isEmpty(sortClause)) {
            result = createDefaultSort(hasScore);
        } else {
            result = createSort(sortClause);
        }

        String documentIdFieldName = getSolrDocumentIdFieldName();
        if (result != null && result.getOrderFor(documentIdFieldName) == null) {
            result = result.and(new Sort(Sort.Direction.ASC, documentIdFieldName));
        }
        return result;
    }

    public Sort createSort(String sortClause) {
        return convertToSolrSort(parseSortClause(sortClause));
    }

    protected abstract Sort createDefaultSort(boolean hasScore);

    protected abstract String getSolrDocumentIdFieldName();

    protected abstract String getSolrSortFieldName(String name);

    protected abstract UniProtDataType getUniProtDataType();

    protected SearchFieldConfig getSearchFieldConfig(UniProtDataType dataType) {
        return SearchFieldConfigFactory.getSearchFieldConfig(dataType);
    }

    protected List<Pair<String, Sort.Direction>> parseSortClause(String sortClause) {
        List<Pair<String, Sort.Direction>> fieldSortPairs = new ArrayList<>();

        String[] tokenizedSortClause =
                sortClause.split("\\s*,\\s*"); // e.g. field1 asc, field2 desc, field3 asc
        boolean hasIdField = false;
        for (String singleSortPairStr : tokenizedSortClause) {
            String[] fieldSortPairArr = singleSortPairStr.split("\\s+");
            if (fieldSortPairArr.length != 2) {
                throw new IllegalArgumentException("You must pass field and sort value in pair.");
            }
            String solrFieldName = getSolrSortFieldName(fieldSortPairArr[0]);
            fieldSortPairs.add(
                    new ImmutablePair<>(
                            solrFieldName, Sort.Direction.fromString(fieldSortPairArr[1])));
            if (solrFieldName.equals(getSolrDocumentIdFieldName())) {
                hasIdField = true;
            }
        }
        if (!hasIdField && !fieldSortPairs.isEmpty()) {
            fieldSortPairs.add(
                    new ImmutablePair<>(getSolrDocumentIdFieldName(), Sort.Direction.ASC));
        }
        return fieldSortPairs;
    }

    private Sort convertToSolrSort(List<Pair<String, Sort.Direction>> fieldSortPairs) {
        Sort sort = null;
        for (Pair<String, Sort.Direction> sField : fieldSortPairs) {
            if (sort == null) {
                sort = new Sort(sField.getRight(), sField.getLeft());
            } else {
                sort = sort.and(new Sort(sField.getRight(), sField.getLeft()));
            }
        }
        return sort;
    }
}
