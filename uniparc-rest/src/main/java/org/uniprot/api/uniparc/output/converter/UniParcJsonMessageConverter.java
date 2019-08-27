package org.uniprot.api.uniparc.output.converter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.uniprot.api.rest.output.converter.JsonMessageConverter;
import org.uniprot.core.json.parser.uniparc.UniParcJsonConfig;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.store.search.field.UniParcField;

/**
 *
 * @author jluo
 * @date: 21 Jun 2019
 *
*/

public class UniParcJsonMessageConverter extends JsonMessageConverter<UniParcEntry> {

	public UniParcJsonMessageConverter() {
		super(UniParcJsonConfig.getInstance().getSimpleObjectMapper(), UniParcEntry.class, Arrays.asList(UniParcField.ResultFields.values()));
	}

	@Override
	protected Map<String, List<String>> getFilterFieldMap(String fields) {
		return new HashMap<>();
	}
}

