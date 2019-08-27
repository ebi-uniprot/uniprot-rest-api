package org.uniprot.api.uniref.request;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Strings;

/**
 *
 * @author jluo
 * @date: 27 Aug 2019
 *
*/

public class UniRefFieldsParser {
	private static final String COMMA = "\\s*,\\s*";
	public static Map<String, List<String>> parseForFilters(String fields) {
		if (Strings.isNullOrEmpty(fields)) {
			return Collections.emptyMap();
		}
		Map<String, List<String>> filters = new HashMap<>();
		String tokens[] = fields.split(COMMA);
		for (String token : tokens) {
			filters.put(token, Collections.emptyList());		
		}
		return filters;

	}
}

