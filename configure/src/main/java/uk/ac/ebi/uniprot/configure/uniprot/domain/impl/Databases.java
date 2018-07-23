package uk.ac.ebi.uniprot.configure.uniprot.domain.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import uk.ac.ebi.kraken.interfaces.uniprot.DatabaseCategory;
import uk.ac.ebi.kraken.interfaces.uniprot.DatabaseType;
import uk.ac.ebi.uniprot.configure.uniprot.domain.DatabaseGroup;
import uk.ac.ebi.uniprot.configure.uniprot.domain.Tuple;

public enum Databases {
	INSTANCE;
	private static final String ANY2 = "any";
	private static final String ANY_CROSS_REFERENCE = "Any cross-reference";
	private static final String ANY = "Any";
	private  List< DatabaseGroup> databases = new ArrayList<>();  
 
	Databases(){
		init();
		 }
	
	 void init() {
			databases.add(getAnyGroup());
			for (DatabaseCategory category: DatabaseCategory.values()) {
				if(category!= DatabaseCategory.UNKNOWN) {
					List<DatabaseType> types = DatabaseType.getDatabaseTypes(category);
					List<Tuple> databaseTypes =
							types.stream().map(this::convert)
							.collect(Collectors.toList());
					databases.add(new DatabaseGroupImpl(category.getName(), databaseTypes));
				}
			}
		
	
	}
	 private Tuple convert(DatabaseType type) {
		 return new TupleImpl(type.getName(), type.name().toLowerCase());
	 }
	private DatabaseGroup getAnyGroup() {
		String group = ANY;
		List<Tuple> databases = 
				Arrays.asList(new TupleImpl(ANY_CROSS_REFERENCE, ANY2));
		return new DatabaseGroupImpl(group, databases);
	}
	public List<DatabaseGroup> getDatabases() {
		return databases;
	}
}
