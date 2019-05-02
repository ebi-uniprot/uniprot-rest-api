package uk.ac.ebi.uniprot.api.rest.search;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.domain.Sort;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A class to handle solr sort clause, like parsing, providing default sort order
 *
 * @author sahmad
 */

public abstract class AbstractSolrSortClause {

    public Sort getSort(String sortClause, boolean hasScore) {
        if (StringUtils.isEmpty(sortClause)) {
            return createDefaultSort(hasScore);
        } else {
            return createSort(sortClause);
        }
    }

    protected abstract Sort createDefaultSort(boolean hasScore);


    private Sort createSort(String sortClause) {
        List<Pair<String, Sort.Direction>> fieldSortPairs = parseSortClause(sortClause);
        Sort sort = convertToSolrSort(fieldSortPairs);
        return sort;
    }

    protected List<Pair<String, Sort.Direction>> parseSortClause(String sortClause) {

        if (StringUtils.isEmpty(sortClause)) {
            return Collections.emptyList();
        }

        List<Pair<String, Sort.Direction>> fieldSortPairs = new ArrayList<>();

        String[] tokenizedSortClause = sortClause.split("\\s*,\\s*");//e.g. field1 asc, field2 desc, field3 asc

        for (String singleSortPairStr : tokenizedSortClause) {
            String[] fieldSortPairArr = singleSortPairStr.split("\\s+");
            if (fieldSortPairArr.length != 2) {
                throw new IllegalArgumentException("You must pass field and sort value in pair.");
            }
            fieldSortPairs.add(new ImmutablePair<>(fieldSortPairArr[0], Sort.Direction.fromString(fieldSortPairArr[1])));
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
