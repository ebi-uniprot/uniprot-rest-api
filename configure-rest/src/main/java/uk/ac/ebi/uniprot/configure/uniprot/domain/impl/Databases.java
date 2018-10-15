package uk.ac.ebi.uniprot.configure.uniprot.domain.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import uk.ac.ebi.kraken.interfaces.uniprot.DatabaseCategory;
import uk.ac.ebi.kraken.interfaces.uniprot.DatabaseType;
import uk.ac.ebi.uniprot.configure.uniprot.domain.DatabaseGroup;
import uk.ac.ebi.uniprot.configure.uniprot.domain.Field;
import uk.ac.ebi.uniprot.configure.uniprot.domain.FieldGroup;
import uk.ac.ebi.uniprot.configure.uniprot.domain.Tuple;

public enum Databases {
	INSTANCE;
	private static final String DATABASES2 = " databases";
	private static final String DR = "dr:";
	private static final String ANY2 = "any";
	private static final String ANY_CROSS_REFERENCE = "Any cross-reference";
	private static final String ANY = "Any";
	private List<DatabaseGroup> databases = new ArrayList<>();
	private List<FieldGroup> databaseFields = new ArrayList<>();
	private Map<String, Field> fieldMap = new HashMap<>();
	Databases() {
		init();
	}

	void init() {
		databases.add(getAnyGroup());
		for (DatabaseCategory category : DatabaseCategory.values()) {
			if (category != DatabaseCategory.UNKNOWN) {
				List<DatabaseType> types = DatabaseType.getDatabaseTypes(category);
				List<Tuple> databaseTypes = types.stream().map(this::convertTuple).collect(Collectors.toList());
				databases.add(new DatabaseGroupImpl(category.getName(), databaseTypes));
				List<Field> fields = types.stream().map(this::convertField).collect(Collectors.toList());
				databaseFields.add(new FieldGroupImpl(trimCategory(category.getName()), fields));
			}
		}
		this.fieldMap =this.databaseFields
				.stream()
				.flatMap(val -> val.getFields().stream())
				.collect(
		                Collectors.toMap(Field::getName, Function.identity()));
	}

	private String trimCategory(String category) {
		if(category.endsWith(DATABASES2)) {
			return category.substring(0, category.length()-DATABASES2.length());
		}else
			return category;
	}
	private Field convertField(DatabaseType type) {
		return new FieldImpl(type.getName(), DR+type.name().toLowerCase());
	}
	private Tuple convertTuple(DatabaseType type) {
		return new TupleImpl(type.getName(), type.name().toLowerCase());
	}

	private DatabaseGroup getAnyGroup() {
		String group = ANY;
		List<Tuple> databases = Arrays.asList(new TupleImpl(ANY_CROSS_REFERENCE, ANY2));
		return new DatabaseGroupImpl(group, databases);
	}

	public List<DatabaseGroup> getDatabases() {
		return databases;
	}
	
	public List<FieldGroup> getDatabaseFields() {
		return databaseFields;
	}
	public Optional<Field> getField(String name){
		return Optional.ofNullable(this.fieldMap.get(name));
	}

}
