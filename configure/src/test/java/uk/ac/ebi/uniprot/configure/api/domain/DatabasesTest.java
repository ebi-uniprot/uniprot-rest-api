package uk.ac.ebi.uniprot.configure.api.domain;

import static org.junit.Assert.assertFalse;

import org.junit.jupiter.api.Test;

import uk.ac.ebi.uniprot.configure.uniprot.domain.impl.Databases;
import uk.ac.ebi.uniprot.configure.uniprot.domain.impl.GoEvidences;

class DatabasesTest {

	@Test
	void test() {
		Databases instance = Databases.INSTANCE;
		assertFalse(instance.getDatabases().isEmpty());
		instance.getDatabases().forEach(System.out::println);
	}

}
