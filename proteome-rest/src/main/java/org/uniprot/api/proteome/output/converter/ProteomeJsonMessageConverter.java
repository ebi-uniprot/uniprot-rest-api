package org.uniprot.api.proteome.output.converter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.uniprot.api.rest.output.converter.JsonMessageConverter;
import org.uniprot.core.json.parser.proteome.ProteomeJsonConfig;
import org.uniprot.core.proteome.ProteomeEntry;
import org.uniprot.store.search.field.ProteomeField;

/**
 *
 * @author jluo
 * @date: 29 Apr 2019
 *
 */

public class ProteomeJsonMessageConverter extends JsonMessageConverter<ProteomeEntry> {

	public ProteomeJsonMessageConverter() {
		super(ProteomeJsonConfig.getInstance().getFullObjectMapper(), ProteomeEntry.class, Arrays.asList(ProteomeField.ResultFields.values()));
	}

	@Override
	protected Map<String, List<String>> getFilterFieldMap(String fields) {
		return new HashMap<>(); //TODO: Filters are not being applied for proteome entry in JSON FORMAT.....
	}
}
