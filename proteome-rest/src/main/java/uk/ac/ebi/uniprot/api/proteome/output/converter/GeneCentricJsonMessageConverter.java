package uk.ac.ebi.uniprot.api.proteome.output.converter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.ebi.uniprot.api.rest.output.converter.JsonMessageConverter;
import uk.ac.ebi.uniprot.domain.proteome.CanonicalProtein;
import uk.ac.ebi.uniprot.json.parser.proteome.ProteomeJsonConfig;
import uk.ac.ebi.uniprot.search.field.GeneCentricField;

/**
 *
 * @author jluo
 * @date: 21 Jun 2019
 *
*/

public class GeneCentricJsonMessageConverter extends JsonMessageConverter<CanonicalProtein> {

	public GeneCentricJsonMessageConverter() {
		super(ProteomeJsonConfig.getInstance().getFullObjectMapper(), CanonicalProtein.class, Arrays.asList(GeneCentricField.ResultFields.values()));
	}

	@Override
	protected Map<String, List<String>> getFilterFieldMap(String fields) {
		return new HashMap<>();
	}
}

