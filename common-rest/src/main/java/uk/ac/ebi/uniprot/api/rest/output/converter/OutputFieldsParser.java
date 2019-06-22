package uk.ac.ebi.uniprot.api.rest.output.converter;

import java.util.Arrays;
import java.util.List;

import com.google.common.base.Strings;

/**
 *
 * @author jluo
 * @date: 21 Jun 2019
 *
*/

public class OutputFieldsParser {
	private static final String COMMA = "\\s*,\\s*";
	public static List<String> parse(String fields, String defaultFields) {
		if (Strings.isNullOrEmpty(fields)) {
			fields = defaultFields;
		}
		return Arrays.asList( fields.split(COMMA));
	}
}

