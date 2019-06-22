package uk.ac.ebi.uniprot.api.uniparc.service;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import uk.ac.ebi.uniprot.domain.uniparc.UniParcEntry;
import uk.ac.ebi.uniprot.json.parser.uniparc.UniParcJsonConfig;
import uk.ac.ebi.uniprot.search.document.uniparc.UniParcDocument;

/**
 *
 * @author jluo
 * @date: 21 Jun 2019
 *
*/

public class UniParcEntryConverter implements Function<UniParcDocument, UniParcEntry> {
	private static final Logger LOGGER = LoggerFactory.getLogger(UniParcEntryConverter.class);

	private final ObjectMapper objectMapper;
	
	public UniParcEntryConverter() {
		objectMapper = UniParcJsonConfig.getInstance().getFullObjectMapper();
	}


	@Override
	public UniParcEntry apply(UniParcDocument t) {
		try {
			UniParcEntry entry= objectMapper.readValue(t.getEntryStored().array(), UniParcEntry.class);
				return entry;
		} catch (Exception e) {
			LOGGER.info("Error converting solr avro_binary default UniProtEntry", e);
		}
		return null;
	}


}

