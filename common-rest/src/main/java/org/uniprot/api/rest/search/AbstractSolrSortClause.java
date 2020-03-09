package org.uniprot.api.rest.search;

import static java.util.stream.Collectors.reducing;

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
    protected List<Pair<String, Sort.Direction>> defaultFieldSortOrderPairs;

    public Sort getSort(String sortClause) {
        return StringUtils.isEmpty(sortClause) ? createDefaultSort() : createSort(sortClause);
    }

    protected abstract List<Pair<String, Sort.Direction>> getDefaultFieldSortOrderPairs();

    protected abstract String getSolrDocumentIdFieldName();

    protected abstract UniProtDataType getUniProtDataType();

    protected String getSolrSortFieldName(String searchFieldName) {
        UniProtDataType dataType = getUniProtDataType();
        SearchFieldConfig searchFieldConfig = getSearchFieldConfig(dataType);
        return searchFieldConfig.getCorrespondingSortField(searchFieldName).getFieldName();
    }

    protected Sort createSort(String sortClause) {
        return convertToSolrSort(parseSortClause(sortClause));
    }

    protected Sort createDefaultSort() {
        List<Pair<String, Sort.Direction>> fieldSortList = getDefaultFieldSortOrderPairs();
        Sort result =
                fieldSortList.stream()
                        .map(this::createSortObject)
                        .collect(reducing(new Sort(Sort.Direction.DESC, SCORE), Sort::and));
        return result;
    }

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

        if (fieldSortPairs.isEmpty()) { // sort by default fields
            fieldSortPairs = getDefaultFieldSortOrderPairs();
        } else if (!hasIdField) {
            fieldSortPairs.add(
                    new ImmutablePair<>(getSolrDocumentIdFieldName(), Sort.Direction.ASC));
        }

        return fieldSortPairs;
    }

    private Sort convertToSolrSort(List<Pair<String, Sort.Direction>> fieldSortPairs) {
        Sort sort = null;
        for (Pair<String, Sort.Direction> sField : fieldSortPairs) {
            if (sort == null) {
                sort = createSortObject(sField);
            } else {
                sort = sort.and(createSortObject(sField));
            }
        }
        return sort;
    }

    private Sort createSortObject(Pair<String, Sort.Direction> fieldSortPair) {
        String sortFieldName = fieldSortPair.getLeft();
        Sort.Direction sortOrder = fieldSortPair.getRight();
        return new Sort(sortOrder, sortFieldName);
    }
}
