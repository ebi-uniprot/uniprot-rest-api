package uk.ac.ebi.uniprot.api.proteome.output.converter;

import uk.ac.ebi.uniprot.api.rest.output.converter.JsonMessageConverter;
import uk.ac.ebi.uniprot.domain.proteome.ProteomeEntry;
import uk.ac.ebi.uniprot.json.parser.proteome.ProteomeJsonConfig;
import uk.ac.ebi.uniprot.search.field.ProteomeField;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
