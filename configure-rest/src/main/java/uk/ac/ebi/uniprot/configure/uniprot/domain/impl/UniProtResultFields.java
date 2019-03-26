package uk.ac.ebi.uniprot.configure.uniprot.domain.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.ac.ebi.uniprot.configure.uniprot.domain.Field;
import uk.ac.ebi.uniprot.configure.uniprot.domain.FieldGroup;

import java.io.InputStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum UniProtResultFields {
	INSTANCE;
	private static final String FILENAME = "uniprot/result_field.json";
	private List<FieldGroup> resultFields = new ArrayList<>();
	private Map<String, Field> fieldMap = new HashMap<>();

	UniProtResultFields() {
		init();
	}

	void init() {
		ObjectMapper objectMapper = JsonConfig.getJsonMapper();
		try (InputStream is = UniProtResultFields.class.getClassLoader().getResourceAsStream(FILENAME);) {
			List<FieldGroupImpl> fields = objectMapper.readValue(is,
					new TypeReference<List<FieldGroupImpl>>() {
					});
			this.resultFields.addAll(fields);
			this.fieldMap =					
					this.resultFields
			.stream()
			.flatMap(val -> val.getFields().stream())
			.collect(
	                Collectors.toMap(Field::getName, Function.identity()));
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
	
	public Optional<Field> getField(String name){
		Field field = this.fieldMap.get(name);
		if(field ==null)
			return Databases.INSTANCE.getField(name);
		else
			return Optional.of(field);
	}

	@Override
	public String toString() {
		return resultFields.stream().map(val -> val.toString()).collect(Collectors.joining(",\n"));
	}

}
