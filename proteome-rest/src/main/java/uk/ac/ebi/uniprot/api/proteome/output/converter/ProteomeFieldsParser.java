package uk.ac.ebi.uniprot.api.proteome.output.converter;

import java.util.Arrays;
import java.util.List;

import com.google.common.base.Strings;

/**
 *
 * @author jluo
 * @date: 1 May 2019
 *
*/

public class ProteomeFieldsParser {
	private static String DEFAULT_FIELDS = "upid,organism,organism_id,protein_count";
	private static final String COMMA = "\\s*,\\s*";
	public static List<String> parse(String fields) {
		if (Strings.isNullOrEmpty(fields)) {
			fields = DEFAULT_FIELDS;
		}
		return Arrays.asList( fields.split(COMMA));
	}
}

