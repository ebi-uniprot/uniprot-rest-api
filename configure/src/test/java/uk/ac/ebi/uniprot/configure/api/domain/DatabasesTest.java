package uk.ac.ebi.uniprot.configure.api.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import uk.ac.ebi.uniprot.configure.uniprot.domain.DatabaseGroup;
import uk.ac.ebi.uniprot.configure.uniprot.domain.Tuple;
import uk.ac.ebi.uniprot.configure.uniprot.domain.impl.Databases;

class DatabasesTest {
	private static Databases instance;

	@BeforeAll
	static void initAll() {
		instance = Databases.INSTANCE;
	}

	@Test
	void testSize() {
		List<DatabaseGroup> groups = instance.getDatabases();
		assertEquals(18, groups.size());
		int nDb = groups.stream().mapToInt(val -> val.getItems().size()).sum();
		assertEquals(155, nDb);
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
