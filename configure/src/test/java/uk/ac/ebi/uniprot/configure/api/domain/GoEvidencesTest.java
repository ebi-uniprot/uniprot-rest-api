package uk.ac.ebi.uniprot.configure.api.domain;

import static org.junit.Assert.assertFalse;

import org.junit.jupiter.api.Test;

import uk.ac.ebi.uniprot.configure.uniprot.domain.impl.GoEvidences;

class GoEvidencesTest {

	@Test
	void test() {
		GoEvidences instance = GoEvidences.INSTANCE;
		assertFalse(instance.getEvidences().isEmpty());
		System.out.println(instance.getEvidences().toString());
	}

}
