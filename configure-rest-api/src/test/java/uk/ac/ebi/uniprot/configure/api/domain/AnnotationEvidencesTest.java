package uk.ac.ebi.uniprot.configure.api.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import uk.ac.ebi.uniprot.configure.uniprot.domain.EvidenceGroup;
import uk.ac.ebi.uniprot.configure.uniprot.domain.EvidenceItem;
import uk.ac.ebi.uniprot.configure.uniprot.domain.impl.AnnotationEvidences;

class AnnotationEvidencesTest {

	private static AnnotationEvidences instance;

	@BeforeAll
	static void initAll() {
		instance = AnnotationEvidences.INSTANCE;
	}

	@Test
	void testSize() {
		List<EvidenceGroup> groups = instance.getEvidences();
		assertEquals(3, groups.size());
		int size =
				groups.stream().mapToInt(val ->val.getItems().size())
				.sum();
		assertEquals(15, size);
	}
	@Test
	void testEvidenceGroup() {
		List<EvidenceGroup> groups = instance.getEvidences();
		assertEquals(3, groups.size());
		assertTrue(groups.stream().anyMatch(val -> val.getGroupName().equals("Any")));
		assertTrue(groups.stream().anyMatch(val -> val.getGroupName().equals("Manual assertions")));

	}

	@Test
	void testEvidence() {
		List<EvidenceGroup> groups = instance.getEvidences();
		Optional<EvidenceGroup> aaGroup = groups.stream()
				.filter(val -> val.getGroupName().equals("Automatic assertions")).findFirst();

		assertTrue(aaGroup.isPresent());
		assertEquals(4, aaGroup.get().getItems().size());
		Optional<EvidenceItem> item = aaGroup.get().getItems().stream()
				.filter(val -> val.getName().equals("Sequence model")).findFirst();
		assertTrue(item.isPresent());
		assertEquals("ECO_0000256", item.get().getCode());
	}

}
