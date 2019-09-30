package org.uniprot.api.uniprotkb.view.service;

import org.junit.jupiter.api.Test;
import org.uniprot.core.cv.pathway.UniPathwayService;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ViewByServiceConfigTest {
    @Test
    void testUniPathwayService() {
        UniPathwayService service = new ViewByServiceConfig().pathwayService();
        assertNotNull(service);
    }

    // TODO: 30/09/19 this class needs to test something
}
