package org.uniprot.api.uniprotkb.view.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.uniprotkb.view.TaxonomyNode;

class TaxonomyServiceTest {

    @Test
    void test() {
        String taxId = "9605";
        TaxonomyService client = new TaxonomyService(new RestTemplate());
        List<TaxonomyNode> nodes = client.getChildren(taxId);
        assertNotNull(nodes);
        assertFalse(nodes.isEmpty());
    }
}
