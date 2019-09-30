package org.uniprot.api.uniprotkb.view.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.uniprot.core.cv.pathway.UniPathwayService;

class ViewByServiceConfigTest {

    @Test
    void testUniPathwayService() {
        UniPathwayService service = new ViewByServiceConfig().pathwayService();
        assertNotNull(service);
    }
}
