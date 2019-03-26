package uk.ac.ebi.uniprot.configure.uniprot.domain.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.ac.ebi.uniprot.configure.uniprot.domain.EvidenceGroup;
import uk.ac.ebi.uniprot.configure.uniprot.domain.SearchEvidences;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public enum GoEvidences implements SearchEvidences {
	INSTANCE;
	private final String FILENAME = "uniprot/go_evidence.json";
	private List<EvidenceGroup> evidences = new ArrayList<>();

	GoEvidences() {
		init();
	}

	void init() {
		ObjectMapper objectMapper = JsonConfig.getJsonMapper();
		try (InputStream is = GoEvidences.class.getClassLoader().getResourceAsStream(FILENAME);) {
			List<EvidenceGroupImpl> evidences = objectMapper.readValue(is,
					new TypeReference<List<EvidenceGroupImpl>>() {
					});
			this.evidences.addAll(evidences);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public List<EvidenceGroup> getEvidences() {
		return evidences;
	}

}
