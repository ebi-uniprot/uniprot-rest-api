package uk.ac.ebi.uniprot.configure.uniprot.domain.impl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.ac.ebi.uniprot.configure.uniprot.domain.FieldGroup;

public enum UniProtResultFields {
	INSTANCE;
	private static final String FILENAME = "uniprot/result_field.json";
	private List<FieldGroup> resultFields = new ArrayList<>();

	UniProtResultFields() {
		init();
	}

	void init() {
		final ObjectMapper objectMapper = new ObjectMapper();
		try (InputStream is = UniProtResultFields.class.getClassLoader().getResourceAsStream(FILENAME);) {
			List<FieldGroupImpl> fields = objectMapper.readValue(is,
					new TypeReference<List<FieldGroupImpl>>() {
					});
			this.resultFields.addAll(fields);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	public List<FieldGroup> getDatabaseFields() {
		return Databases.INSTANCE.getDatabaseFields();
	} 
	public List<FieldGroup> getResultFields() {
		return resultFields;
	}

	@Override
	public String toString() {
		return resultFields.stream().map(val -> val.toString()).collect(Collectors.joining(",\n"));
	}

}
