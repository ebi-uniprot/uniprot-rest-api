package uk.ac.ebi.uniprot.api.uniparc.output.converter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.ebi.uniprot.api.rest.output.converter.JsonMessageConverter;
import uk.ac.ebi.uniprot.domain.uniparc.UniParcEntry;
import uk.ac.ebi.uniprot.json.parser.uniparc.UniParcJsonConfig;
import uk.ac.ebi.uniprot.search.field.UniParcField;

/**
 *
 * @author jluo
 * @date: 21 Jun 2019
 *
*/

public class UniParcJsonMessageConverter extends JsonMessageConverter<UniParcEntry> {

	public UniParcJsonMessageConverter() {
		super(UniParcJsonConfig.getInstance().getFullObjectMapper(), UniParcEntry.class, Arrays.asList(UniParcField.ResultFields.values()));
	}

	@Override
	protected Map<String, List<String>> getFilterFieldMap(String fields) {
		return new HashMap<>();
	}
}

