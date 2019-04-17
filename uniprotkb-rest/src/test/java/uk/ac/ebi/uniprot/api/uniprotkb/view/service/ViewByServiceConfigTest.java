package uk.ac.ebi.uniprot.api.uniprotkb.view.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import uk.ac.ebi.uniprot.cv.pathway.UniPathwayService;

class ViewByServiceConfigTest {

	@Test
	void testUniPathwayService() {
		UniPathwayService service = new ViewByServiceConfig().pathwayService();
		assertNotNull(service);
	}

}
