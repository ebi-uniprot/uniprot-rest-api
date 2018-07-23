package uk.ac.ebi.uniprot.configure.api.domain;

import static org.junit.Assert.assertFalse;

import org.junit.jupiter.api.Test;

import uk.ac.ebi.uniprot.configure.uniprot.domain.impl.AnnotationEvidences;

class AnnotationEvidencesTest {

	@Test
	public void test() {
		AnnotationEvidences instance = AnnotationEvidences.INSTANCE;
		assertFalse(instance.getEvidences().isEmpty());
	}

}
