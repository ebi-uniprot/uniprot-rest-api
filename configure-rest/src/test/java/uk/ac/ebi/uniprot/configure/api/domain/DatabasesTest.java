package uk.ac.ebi.uniprot.configure.api.domain;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.ac.ebi.uniprot.configure.uniprot.domain.DatabaseGroup;
import uk.ac.ebi.uniprot.configure.uniprot.domain.Field;
import uk.ac.ebi.uniprot.configure.uniprot.domain.Tuple;
import uk.ac.ebi.uniprot.configure.uniprot.domain.impl.Databases;

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
	void testSize() {
		List<DatabaseGroup> groups = instance.getDatabases();
		assertEquals(18, groups.size());
		int nDb = groups.stream().mapToInt(val -> val.getItems().size()).sum();
		assertEquals(156, nDb);
		instance.getDatabases().forEach(System.out::println);
	}
	@Test
	void testGroup() {
		List<DatabaseGroup> groups = instance.getDatabases();
		assertTrue(groups.stream().anyMatch(val -> val.getGroupName().equals("Sequence databases")));
		assertTrue(groups.stream().anyMatch(val -> val.getGroupName().equals("Genome annotation databases")));
		assertFalse(groups.stream().anyMatch(val -> val.getGroupName().equals("Proteomic database")));

	}
	@Test
	void testDatabase() {
		List<DatabaseGroup> groups = instance.getDatabases();
		Optional<DatabaseGroup> ppGroup = groups.stream()
				.filter(val -> val.getGroupName().equals("Protein family/group databases")).findFirst();

		assertTrue(ppGroup.isPresent());
		assertEquals(12, ppGroup.get().getItems().size());
		Optional<Tuple> item = ppGroup.get().getItems().stream()
				.filter(val -> val.getName().equals("MoonProt")).findFirst();
		assertTrue(item.isPresent());
		assertEquals("moonprot", item.get().getValue());
	}
}
