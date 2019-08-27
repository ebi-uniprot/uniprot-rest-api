package org.uniprot.api.uniref.output.converter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.uniprot.api.rest.output.converter.JsonMessageConverter;
import org.uniprot.core.json.parser.uniref.UniRefEntryJsonConfig;
import org.uniprot.core.uniref.UniRefEntry;
import org.uniprot.store.search.field.UniRefField;

/**
 *
 * @author jluo
 * @date: 22 Aug 2019
 *
*/

public class UniRefJsonMessageConverter extends JsonMessageConverter<UniRefEntry> {
	public UniRefJsonMessageConverter() {
		super(UniRefEntryJsonConfig.getInstance().getSimpleObjectMapper(), UniRefEntry.class, 
				Arrays.asList(UniRefField.ResultFields.values()));
	}

	@Override
	protected Map<String, List<String>> getFilterFieldMap(String fields) {
		
		return new HashMap<>();
	}
	@Override
	protected Map<String, Object> projectEntryFields(UniRefEntry entity) {
		 Map<String, Object> map = new HashMap<>();
		 map.put("uniref", entity);
		 return map;
	}
}

