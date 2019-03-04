package uk.ac.ebi.uniprot.uniprotkb.service;

import com.google.common.base.Strings;
import org.springframework.data.domain.Sort;
import uk.ac.ebi.uniprot.uniprotkb.configuration.UniProtField;

public class UniProtSortUtil {

    private static Sort addSort(Sort initialSort, Sort.Direction direction, UniProtField.Sort fields) {
        Sort newSort = new Sort(direction, fields.getSolrFieldName());
        if (initialSort != null) {
            return initialSort.and(newSort);
        } else {
            return newSort;
        }
    }

    public static Sort createSort(String sortFields) {
        if (Strings.isNullOrEmpty(sortFields)) {
            return null;
        }
        Sort sort = new Sort(Sort.Direction.DESC, "score");
        boolean hasAccession = false;
        String[] tokens = sortFields.split("\\s*,\\s*");
        for (String token : tokens) {
            String[] sortedField = token.split("\\s+");
            if (sortedField[0].equals(UniProtField.Sort.accession.name()))
                hasAccession = true;
            if (isDescendentSort(sortedField)) {
                sort = addSort(sort, Sort.Direction.DESC, UniProtField.Sort.valueOf(sortedField[0]));
            } else {
                sort = addSort(sort, Sort.Direction.ASC, UniProtField.Sort.valueOf(sortedField[0]));
            }
        }
        if (!hasAccession) {
            sort = addSort(sort, Sort.Direction.ASC, UniProtField.Sort.accession);
        }
        return sort;
    }

    private static boolean isDescendentSort(String[] sortedField) {
        return (sortedField.length == 2) && sortedField[1].equalsIgnoreCase(Sort.Direction.DESC.name());
    }

    public static Sort createDefaultSort() {
        return new Sort(Sort.Direction.DESC, "score")
                .and(new Sort(Sort.Direction.DESC, UniProtField.Sort.annotation_score.getSolrFieldName()))
                .and(new Sort(Sort.Direction.ASC, UniProtField.Sort.accession.getSolrFieldName()));
    }

}
