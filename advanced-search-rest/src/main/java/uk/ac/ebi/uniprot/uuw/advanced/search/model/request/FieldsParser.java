package uk.ac.ebi.uniprot.uuw.advanced.search.model.request;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Strings;

public class FieldsParser {
	private static final String COLON = ":";
	private static final String ALL = "all";
	private static final String XREF = "xref";
	private static final String FEATURE = "feature";
	private static final String COMMENT = "comment";
	private static final String COMMA = ",";

	public static Map<String, List<String> > parse(String fields){
		if(Strings.isNullOrEmpty(fields)) {
			return Collections.emptyMap();
		}
		Map<String, List<String> > filters = new HashMap<>();
		String tokens[] = fields.split(COMMA);
		for(String token:tokens) {
			if(token.startsWith(COMMENT)) {
				addTypedField(filters, COMMENT, token);
				
			}else if(token.startsWith(FEATURE)) {
				addTypedField(filters, FEATURE, token);
			}else if(token.startsWith(XREF)) {
				addTypedField(filters, XREF, token);
			}else {
				filters.put(token, Collections.emptyList());
			}
		}
		return filters;
		
	}
	private static void addTypedField(Map<String, List<String> > filters, String type, String token) {
		if(token.equals(type)) {
			putMap(filters, token, ALL);
		}else if(token.startsWith(type + COLON)) {
			String value = token.substring(token.indexOf(COLON)+1);
			putMap(filters, type, value);
			
		}else {
			filters.put(token, Collections.emptyList());
		}
	}
	private static void putMap(Map<String, List<String> > filters, String key, String value) {
		List<String> values = filters.get(key);
		if(values ==null) {
			values = new ArrayList<>();
			filters.put(key, values);
		}
		values.add(value);
	}
}
