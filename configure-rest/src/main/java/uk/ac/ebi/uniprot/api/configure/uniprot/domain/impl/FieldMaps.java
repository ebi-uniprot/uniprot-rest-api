package uk.ac.ebi.uniprot.api.configure.uniprot.domain.impl;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public enum FieldMaps {
	INSTANCE;
	private static final String FILENAME = "uniprot/field_map.json";
	private Map<String, String> fieldMap = new HashMap<>();

	FieldMaps() {
		init();
	}

	void init() {
		final ObjectMapper objectMapper = new ObjectMapper();
		try (InputStream is = UniProtResultFields.class.getClassLoader().getResourceAsStream(FILENAME);) {
			List<FieldMap> fields = objectMapper.readValue(is,
					new TypeReference<List<FieldMap>>() {
					});
			for(FieldMap field: fields) {
				fieldMap.put(field.getName(), field.getMapTo());
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}
	
	public String getField(String name) {
		return fieldMap.getOrDefault(name, name);
	}
	
	public static class FieldMap{
		private String name;
		private String mapTo;
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getMapTo() {
			return mapTo;
		}
		public void setMapTo(String mapTo) {
			this.mapTo = mapTo;
		}
		
	}
}
