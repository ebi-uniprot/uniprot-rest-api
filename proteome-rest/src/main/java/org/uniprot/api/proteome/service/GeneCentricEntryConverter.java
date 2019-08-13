package org.uniprot.api.proteome.service;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uniprot.core.json.parser.proteome.ProteomeJsonConfig;
import org.uniprot.core.proteome.CanonicalProtein;
import org.uniprot.store.search.document.proteome.GeneCentricDocument;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * @author jluo
 * @date: 17 May 2019
 *
*/

public class GeneCentricEntryConverter implements Function<GeneCentricDocument, CanonicalProtein> {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProteomeEntryConverter.class);

	private final ObjectMapper objectMapper;
	
	public GeneCentricEntryConverter() {
		objectMapper = ProteomeJsonConfig.getInstance().getFullObjectMapper();
	}

	

	@Override
	public CanonicalProtein apply(GeneCentricDocument t) {
		try {
			CanonicalProtein entry= objectMapper.readValue(t.getGeneCentricStored().array(),
					org.uniprot.core.proteome.CanonicalProtein.class);
				return entry;
		} catch (Exception e) {
			LOGGER.info("Error converting solr avro_binary default UniProtEntry", e);
		}
		return null;
	}
	
}
