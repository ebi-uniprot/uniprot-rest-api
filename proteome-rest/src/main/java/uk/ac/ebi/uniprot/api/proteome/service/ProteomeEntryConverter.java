package uk.ac.ebi.uniprot.api.proteome.service;

import java.util.Collections;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import uk.ac.ebi.uniprot.domain.proteome.ProteomeEntry;
import uk.ac.ebi.uniprot.domain.proteome.builder.ProteomeEntryBuilder;
import uk.ac.ebi.uniprot.json.parser.proteome.ProteomeJsonConfig;
import uk.ac.ebi.uniprot.search.document.proteome.ProteomeDocument;

/**
 *
 * @author jluo
 * @date: 29 Apr 2019
 *
 */
public class ProteomeEntryConverter implements Function<ProteomeDocument, ProteomeEntry> {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProteomeEntryConverter.class);

	private final ObjectMapper objectMapper;
	private final boolean filterGene;
	
	public ProteomeEntryConverter() {
		this(true);
	}

	public ProteomeEntryConverter(boolean filterGene) {
		this.filterGene =filterGene;
		objectMapper = ProteomeJsonConfig.getInstance().getFullObjectMapper();
		
	}

	@Override
	public ProteomeEntry apply(ProteomeDocument t) {
		try {
			ProteomeEntry entry= objectMapper.readValue(t.proteomeStored.array(),
					uk.ac.ebi.uniprot.domain.proteome.ProteomeEntry.class);
			if(filterGene) {
				return filterGene(entry);
			}else
				return entry;
		} catch (Exception e) {
			LOGGER.info("Error converting solr avro_binary default UniProtEntry", e);
		}
		return null;
	}
	private ProteomeEntry filterGene(ProteomeEntry entry) {
		if(entry.getCanonicalProteins().isEmpty())
			return entry;
		ProteomeEntryBuilder builder = new ProteomeEntryBuilder().from(entry);
		builder.canonicalProteins(Collections.emptyList());
		return builder.build();
	}

}
