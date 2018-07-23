package uk.ac.ebi.uniprot.configure.api.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import uk.ac.ebi.uniprot.configure.uniprot.domain.impl.UniProtSearchItem;
import uk.ac.ebi.uniprot.configure.uniprot.domain.impl.UniProtSearchItems;

class UniProtConfigureServiceTest {
	private static UniProtConfigureService service;
	@BeforeAll
	static void initAll() {
		service = new UniProtConfigureService();
	}
	@Test
	void testGetUniProtSearchItems() {
		UniProtSearchItems searchItems =service.getUniProtSearchItems();
		List<UniProtSearchItem> items =searchItems.getSearchItems();
		assertEquals(27, items.size());
		
	}

	@Test
	void testGetAnnotationEvidences() {
		fail("Not yet implemented");
	}

	@Test
	void testGetGoEvidences() {
		fail("Not yet implemented");
	}

	@Test
	void testGetDatabases() {
		fail("Not yet implemented");
	}

}
