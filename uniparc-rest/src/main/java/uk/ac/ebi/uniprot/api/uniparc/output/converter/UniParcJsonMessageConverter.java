package uk.ac.ebi.uniprot.api.uniparc.output.converter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.ebi.uniprot.api.rest.output.converter.AbstractJsonMessageConverter;
import uk.ac.ebi.uniprot.domain.uniparc.UniParcEntry;
import uk.ac.ebi.uniprot.json.parser.uniparc.UniParcJsonConfig;

/**
 *
 * @author jluo
 * @date: 21 Jun 2019
 *
*/

public class UniParcJsonMessageConverter extends AbstractJsonMessageConverter<UniParcEntry> {

	public UniParcJsonMessageConverter() {
		super(UniParcJsonConfig.getInstance().getFullObjectMapper(), UniParcEntry.class);
	}

	@Override
	protected UniParcEntry filterEntryContent(UniParcEntry entity) {
		return entity;
	}
	@Override
	protected Map<String, List<String>> getFilterFieldMap(String fields) {
		return new HashMap<>(); 
	}
}

