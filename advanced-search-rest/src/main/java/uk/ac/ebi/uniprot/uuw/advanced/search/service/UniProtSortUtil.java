package uk.ac.ebi.uniprot.uuw.advanced.search.service;

import com.google.common.base.Strings;
import org.springframework.data.domain.Sort;
import uk.ac.ebi.uniprot.dataservice.client.uniprot.UniProtField;

import java.util.Optional;

public class UniProtSortUtil {

    private static Sort addSort(Sort initialSort, Sort.Direction direction, UniProtField.Sort fields) {
        Sort newSort = new Sort(direction, fields.getSolrFieldName());
        if (initialSort != null) {
            return initialSort.and(newSort);
        } else {
            return newSort;
        }
    }

    public static Optional<Sort> createSort(String sortFields) {
        if (Strings.isNullOrEmpty(sortFields)) {
            return Optional.empty();
        }
        Sort sort = null;
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
        if (sort != null && !hasAccession) {
            sort = addSort(sort, Sort.Direction.ASC, UniProtField.Sort.accession);
        }

        if (sort == null) {
            return Optional.empty();
        } else {
            return Optional.of(sort);
        }
    }

    private static boolean isDescendentSort(String[] sortedField) {
        return (sortedField.length == 2) && sortedField[1].equalsIgnoreCase(Sort.Direction.DESC.name());
    }

    public static Sort createDefaultSort() {
        return new Sort(Sort.Direction.DESC, UniProtField.Sort.annotation_score.getSolrFieldName())
                .and(new Sort(Sort.Direction.ASC, UniProtField.Sort.accession.getSolrFieldName()));
    }

}
