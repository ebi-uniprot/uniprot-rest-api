package uk.ac.ebi.uniprot.api.configure.proteome;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.ac.ebi.uniprot.api.configure.uniprot.domain.Field;
import uk.ac.ebi.uniprot.api.configure.uniprot.domain.FieldGroup;
import uk.ac.ebi.uniprot.api.configure.uniprot.domain.impl.FieldGroupImpl;
import uk.ac.ebi.uniprot.api.configure.uniprot.domain.impl.JsonConfig;

/**
 *
 * @author jluo
 * @date: 30 Apr 2019
 *
 */

public enum ProteomeResultFields {
	INSTANCE;
	private static final String FILENAME = "proteome/prtoeome_result_field.json";
	private List<FieldGroup> resultFields = new ArrayList<>();
	private Map<String, Field> fieldMap = new HashMap<>();

	ProteomeResultFields() {
		init();
	}

	void init() {
		ObjectMapper objectMapper = JsonConfig.getJsonMapper();
		try (InputStream is = ProteomeResultFields.class.getClassLoader().getResourceAsStream(FILENAME);) {
			List<FieldGroupImpl> fields = objectMapper.readValue(is, new TypeReference<List<FieldGroupImpl>>() {
			});
			this.resultFields.addAll(fields);
			this.fieldMap = this.resultFields.stream().flatMap(val -> val.getFields().stream())
					.collect(Collectors.toMap(Field::getName, Function.identity()));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	public List<FieldGroup> getResultFields() {
		return resultFields;
	}

	public Optional<Field> getField(String name) {
		return Optional.ofNullable( this.fieldMap.get(name));
	}

	@Override
	public String toString() {
		return resultFields.stream().map(val -> val.toString()).collect(Collectors.joining(",\n"));
	}

}
