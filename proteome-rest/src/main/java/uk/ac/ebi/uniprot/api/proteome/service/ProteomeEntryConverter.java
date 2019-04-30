package uk.ac.ebi.uniprot.api.proteome.service;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import uk.ac.ebi.uniprot.domain.proteome.ProteomeEntry;
import uk.ac.ebi.uniprot.json.parser.proteome.ProteomeJsonConfig;
import uk.ac.ebi.uniprot.search.document.proteome.ProteomeDocument;

/**
 *
 * @author jluo
 * @date: 29 Apr 2019
 *
 */
@Service
public class ProteomeEntryConverter implements Function<ProteomeDocument, ProteomeEntry> {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProteomeEntryConverter.class);

	private final ObjectMapper objectMapper;

	public ProteomeEntryConverter() {
		objectMapper = ProteomeJsonConfig.getInstance().getDefaultFullObjectMapper();
	}

	@Override
	public ProteomeEntry apply(ProteomeDocument t) {
		try {
			return objectMapper.readValue(t.proteomeStored.array(),
					uk.ac.ebi.uniprot.domain.proteome.ProteomeEntry.class);
		} catch (Exception e) {
			LOGGER.info("Error converting solr avro_binary default UniProtEntry", e);
		}
		return null;
	}

}
