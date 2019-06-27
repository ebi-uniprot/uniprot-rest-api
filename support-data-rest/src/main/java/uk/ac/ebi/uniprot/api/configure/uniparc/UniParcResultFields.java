package uk.ac.ebi.uniprot.api.configure.uniparc;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.ac.ebi.uniprot.api.configure.proteome.ProteomeResultFields;
import uk.ac.ebi.uniprot.api.configure.uniprot.domain.Field;
import uk.ac.ebi.uniprot.api.configure.uniprot.domain.FieldGroup;
import uk.ac.ebi.uniprot.api.configure.uniprot.domain.impl.FieldGroupImpl;
import uk.ac.ebi.uniprot.api.configure.uniprot.domain.impl.FieldImpl;
import uk.ac.ebi.uniprot.api.configure.uniprot.domain.impl.JsonConfig;
import uk.ac.ebi.uniprot.search.field.ReturnField;

/**
 *
 * @author jluo
 * @date: 20 Jun 2019
 *
*/

public enum UniParcResultFields implements ReturnField {
	INSTANCE;
	private static final String FILENAME = "uniparc/uniparc_result_field.json";
	private List<FieldGroup> resultFields = new ArrayList<>();
	private Map<String, Field> fieldMap = new HashMap<>();
	

	UniParcResultFields() {
		init();
	}

	void init() {
		ObjectMapper objectMapper = JsonConfig.getJsonMapper();
		try (InputStream is = ProteomeResultFields.class.getClassLoader().getResourceAsStream(FILENAME);) {
			List<FieldGroupImpl> fields = objectMapper.readValue(is,
					new TypeReference<List<FieldGroupImpl>>() {
					});
			this.resultFields.addAll(fields);
			
			this.fieldMap.put("upi", new FieldImpl("Entry", "upi"));		
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
	}
}

