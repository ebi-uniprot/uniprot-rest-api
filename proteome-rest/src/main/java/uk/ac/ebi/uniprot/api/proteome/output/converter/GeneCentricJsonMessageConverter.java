package uk.ac.ebi.uniprot.api.proteome.output.converter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.ebi.uniprot.api.rest.output.converter.AbstractJsonMessageConverter;
import uk.ac.ebi.uniprot.domain.proteome.CanonicalProtein;
import uk.ac.ebi.uniprot.json.parser.proteome.ProteomeJsonConfig;

/**
 *
 * @author jluo
 * @date: 21 Jun 2019
 *
*/

public class GeneCentricJsonMessageConverter extends AbstractJsonMessageConverter<CanonicalProtein> {

	public GeneCentricJsonMessageConverter() {
		super(ProteomeJsonConfig.getInstance().getFullObjectMapper(), CanonicalProtein.class);
	}

	@Override
	protected CanonicalProtein filterEntryContent(CanonicalProtein entity) {
		return entity;
	}
	@Override
	protected Map<String, List<String>> getFilterFieldMap(String fields) {
		return new HashMap<>(); 
	}
}

