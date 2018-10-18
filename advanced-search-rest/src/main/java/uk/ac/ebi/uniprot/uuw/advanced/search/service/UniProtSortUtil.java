package uk.ac.ebi.uniprot.uuw.advanced.search.service;

import com.google.common.base.Strings;
import org.springframework.data.domain.Sort;

import java.util.*;

public class UniProtSortUtil {
    private static final String D = "d";
    private static final String ANNOTATION_SCORE = "annotation_score";
    private static final String ACCESSION = "accession";

    private static final Set<String> validSortFields = new HashSet<>();

    static {
        validSortFields.add(ACCESSION);
        validSortFields.add(ANNOTATION_SCORE);
        validSortFields.add("organism");
        validSortFields.add("protein_name");
        validSortFields.add("gene");
        validSortFields.add("length");
        validSortFields.add("mass");
    }

    private static Sort addSort(Sort initialSort, Sort.Direction direction, String... fields) {
        Sort newSort = new Sort(direction, fields);
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
        String[] tokens = sortFields.split(",");
        for (String token : tokens) {
            String[] sortedField = token.split(":");
            if (!isValidSortField(sortedField[0]))
                continue;
            if (sortedField[0].equals(ACCESSION))
                hasAccession = true;
            if ((sortedField.length == 2) && D.equals(sortedField[1])) {
                sort = addSort(sort, Sort.Direction.DESC, sortedField[0]);
            } else {
                sort = addSort(sort, Sort.Direction.ASC, sortedField[0]);
            }
        }
        if (sort != null && !hasAccession) {
            sort = addSort(sort, Sort.Direction.ASC, ACCESSION);
        }

        if (sort == null) {
            return Optional.empty();
        } else {
            return Optional.of(sort);
        }
    }

    public static Sort createDefaultSort() {
        return new Sort(Sort.Direction.DESC, ANNOTATION_SCORE)
                .and(new Sort(Sort.Direction.ASC, ACCESSION));
    }

    public static boolean isValidSortField(String field) {
        return validSortFields.contains(field);
    }
}
