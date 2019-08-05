package org.uniprot.api.configure.proteome;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import org.uniprot.api.configure.uniprot.domain.Field;
import org.uniprot.api.configure.uniprot.domain.FieldGroup;
import org.uniprot.api.configure.uniprot.domain.impl.FieldGroupImpl;
import org.uniprot.api.configure.uniprot.domain.impl.FieldImpl;
import org.uniprot.api.configure.uniprot.domain.impl.JsonConfig;
import org.uniprot.store.search.field.ReturnField;

/**
 *
 * @author jluo
 * @date: 30 Apr 2019
 *
 */

public enum ProteomeResultFields implements ReturnField {
	INSTANCE;
	private static final String FILENAME = "proteome/proteome_result_field.json";
	private List<FieldGroup> resultFields = new ArrayList<>();
	private Map<String, Field> fieldMap = new HashMap<>();
	

	ProteomeResultFields() {
		init();
	}

	void init() {
		ObjectMapper objectMapper = JsonConfig.getJsonMapper();
		try (InputStream is = ProteomeResultFields.class.getClassLoader().getResourceAsStream(FILENAME);) {
			List<FieldGroupImpl> fields = objectMapper.readValue(is,
					new TypeReference<List<FieldGroupImpl>>() {
					});
			this.resultFields.addAll(fields);
			
			this.fieldMap.put("upid", new FieldImpl("Proteome ID", "upid"));		
			this.resultFields.stream().flatMap(val -> val.getFields().stream())
					.forEach(field -> this.fieldMap.put(field.getName(), field));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	public List<FieldGroup> getResultFieldGroups() {
		return resultFields;
	}

	public Optional<Field> getField(String name) {
		return Optional.ofNullable( this.fieldMap.get(name));
	}

	public Map<String, Field> getAllFields(){
		return fieldMap;
	}
	@Override
	public String toString() {
		return resultFields.stream().map(val -> val.toString()).collect(Collectors.joining(",\n"));
	}

	@Override
	public boolean hasReturnField(String fieldName) {
		return INSTANCE.getField(fieldName).isPresent();
	}}
