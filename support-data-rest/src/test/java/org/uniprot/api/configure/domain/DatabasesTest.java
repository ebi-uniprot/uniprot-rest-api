package org.uniprot.api.configure.domain;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.uniprot.api.configure.uniprot.domain.DatabaseGroup;
import org.uniprot.api.configure.uniprot.domain.Field;
import org.uniprot.api.configure.uniprot.domain.Tuple;
import org.uniprot.api.configure.uniprot.domain.impl.Databases;
import org.uniprot.core.cv.xdb.DatabaseCategory;
import org.uniprot.core.cv.xdb.UniProtXDbTypeDetail;
import org.uniprot.core.cv.xdb.UniProtXDbTypes;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

class DatabasesTest {
	private static Databases instance;

	@BeforeAll
	static void initAll() {
		instance = Databases.INSTANCE;
	}
	
	@Test
	void fieldUniqueness() {
		Map<String, List<Field>> result = instance.getDatabaseFields()
				.stream().flatMap(val -> val.getFields().stream()).collect(Collectors.groupingBy(Field::getName));
		
		assertFalse( result.entrySet()
		.stream()
		.anyMatch(val -> val.getValue().size()>1))
		;
		
	
	}
	@Test
	void testField() {
		assertTrue(instance.getField("dr:embl").isPresent());
		assertTrue(instance.getField("dr:ensembl").isPresent());
		assertFalse(instance.getField("embl").isPresent());
	}

	@Test
	void testHasCorrectKnownCrossReferencesSize() {
		List<UniProtXDbTypeDetail> allKnownCrossReferences = UniProtXDbTypes.INSTANCE.getAllDBXRefTypes().stream()
				.filter(dbd -> !dbd.getCategory().equals(DatabaseCategory.UNKNOWN))
				.collect(Collectors.toList());

		List<DatabaseGroup> groups = instance.getDatabases();
		assertEquals(20, groups.size());
		List<Tuple> databaseGroupItems = groups.stream()
				.flatMap(g -> g.getItems().stream())
				.filter(t -> !t.getValue().equals("any"))
				.collect(Collectors.toList());
		assertEquals(allKnownCrossReferences.size(), databaseGroupItems.size());
		int nDb = groups.stream().mapToInt(val -> val.getItems().size()).sum();
		assertTrue(nDb>159);
	}
	@Test
	void testGroup() {
		List<DatabaseGroup> groups = instance.getDatabases();
		assertTrue(groups.stream().anyMatch(val -> val.getGroupName().equals("SEQ")));
		assertTrue(groups.stream().anyMatch(val -> val.getGroupName().equals("GMA")));
		assertFalse(groups.stream().anyMatch(val -> val.getGroupName().equals("Proteomic database")));

	}
	@Test
	void testDatabase() {
		List<DatabaseGroup> groups = instance.getDatabases();
		Optional<DatabaseGroup> ppGroup = groups.stream()
				.filter(val -> val.getGroupName().equals("PFAM")).findFirst();

		assertTrue(ppGroup.isPresent());
		assertEquals(12, ppGroup.get().getItems().size());
		Optional<Tuple> item = ppGroup.get().getItems().stream()
				.filter(val -> val.getName().equals("MoonProt")).findFirst();
		assertTrue(item.isPresent());
		assertEquals("moonprot", item.get().getValue());
	}
}
