package uk.ac.ebi.uniprot.configure.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import uk.ac.ebi.uniprot.configure.uniprot.domain.SearchItem;

class UniProtConfigureServiceTest {
	private static UniProtConfigureService service;
	@BeforeAll
	static void initAll() {
		service = new UniProtConfigureService();
	}
	@Test
	void testGetUniProtSearchItems() {
		List<SearchItem> items =service.getUniProtSearchItems();
		assertEquals(27, items.size());
		
	}

	@Test
	void testGetAnnotationEvidences() {
		assertEquals(3, service.getAnnotationEvidences().size());
	}

	@Test
	void testGetGoEvidences() {
		assertEquals(5, service.getGoEvidences().size());
	}

	@Test
	void testGetDatabases() {
		assertEquals(18, service.getDatabases().size());
	}

}
