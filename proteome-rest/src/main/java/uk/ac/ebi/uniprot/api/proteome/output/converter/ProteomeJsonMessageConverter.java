package uk.ac.ebi.uniprot.api.proteome.output.converter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.ebi.uniprot.api.rest.output.converter.AbstractJsonMessageConverter;
import uk.ac.ebi.uniprot.json.parser.proteome.ProteomeJsonConfig;

/**
 *
 * @author jluo
 * @date: 29 Apr 2019
 *
 */

public class ProteomeJsonMessageConverter extends AbstractJsonMessageConverter<Object> {

	public ProteomeJsonMessageConverter() {
		super(ProteomeJsonConfig.getInstance().getFullObjectMapper());
	}

	@Override
	protected Object filterEntryContent(Object entity) {
		return entity; //TODO: Filters are not being applied for proteome entry in JSON FORMAT.....
	}

	@Override
	protected Map<String, List<String>> getFilterFieldMap(String fields) {
		return new HashMap<>(); //TODO: Filters are not being applied for proteome entry in JSON FORMAT.....
	}
}
