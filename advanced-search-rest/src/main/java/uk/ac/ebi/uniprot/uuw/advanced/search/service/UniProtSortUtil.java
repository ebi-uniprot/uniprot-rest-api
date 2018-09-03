package uk.ac.ebi.uniprot.uuw.advanced.search.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Sort;

import com.google.common.base.Strings;

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
	}

	public static List<Sort> createSort(String sortFields){
		if (Strings.isNullOrEmpty(sortFields)) {
			return Collections.emptyList();
		}
		List<Sort> sorts = new ArrayList<>();
		boolean hasAccession = false;
		String[] tokens = sortFields.split(",");
		for (String token : tokens) {
			String[] sortedField = token.split(":");
			if (!isValidSortField(sortedField[0]))
				continue;
			if (sortedField[0].equals(ACCESSION))
				hasAccession = true;
			if ((sortedField.length == 2) && D.equals(sortedField[1])) {
				sorts.add(new Sort(Sort.Direction.DESC, sortedField[0]));
			} else {
				sorts.add(new Sort(Sort.Direction.ASC, sortedField[0]));
			}
		}
		if(!sorts.isEmpty() && !hasAccession) {
			sorts.add(new Sort(Sort.Direction.ASC, ACCESSION));
		}
		return sorts;
	}
	
	public static List<Sort> createDefaultSort() {
		List<Sort> sorts = new ArrayList<>();
		sorts.add(new Sort(Sort.Direction.DESC, ANNOTATION_SCORE));
		sorts.add(new Sort(Sort.Direction.ASC, ACCESSION));
		return sorts;

	}

	public static boolean isValidSortField(String field) {
		return validSortFields.contains(field);
	}
}
