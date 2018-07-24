package uk.ac.ebi.uniprot.configure.api.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import uk.ac.ebi.uniprot.configure.uniprot.domain.EvidenceGroup;
import uk.ac.ebi.uniprot.configure.uniprot.domain.EvidenceItem;
import uk.ac.ebi.uniprot.configure.uniprot.domain.impl.GoEvidences;

class GoEvidencesTest {
	private static GoEvidences instance;

	@BeforeAll
	static void initAll() {
		instance = GoEvidences.INSTANCE;
	}

	@Test
	void testSize() {
		List<EvidenceGroup> groups = instance.getEvidences();
		assertEquals(5, groups.size());
		int size =
				groups.stream().mapToInt(val ->val.getItems().size())
				.sum();
		assertEquals(25, size);
	}
	
	@Test
	void testEvidenceGroup() {
		List<EvidenceGroup> groups = instance.getEvidences();
		assertEquals(5, groups.size());
		assertTrue(groups.stream().anyMatch(val -> val.getGroupName().equals("Any")));
		assertTrue(groups.stream().anyMatch(val -> val.getGroupName().equals("Manual experimental assertions")));

	}

	@Test
	void testEvidence() {
		List<EvidenceGroup> groups = instance.getEvidences();
		Optional<EvidenceGroup> aaGroup = groups.stream()
				.filter(val -> val.getGroupName().equals("Munual high-throughput assertions")).findFirst();

		assertTrue(aaGroup.isPresent());
		assertEquals(5, aaGroup.get().getItems().size());
		Optional<EvidenceItem> item = aaGroup.get().getItems().stream()
				.filter(val -> val.getName().equals("Inferred from high throughput genetic interaction [HGI]")).findFirst();
		assertTrue(item.isPresent());
		assertEquals("HGI", item.get().getCode());
	}
}
