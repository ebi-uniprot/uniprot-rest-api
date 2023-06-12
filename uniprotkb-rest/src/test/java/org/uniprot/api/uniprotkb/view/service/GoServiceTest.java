package org.uniprot.api.uniprotkb.view.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.uniprotkb.view.GoRelation;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GoServiceTest {

    @Test
    void test() {
        String goId = "GO:0008150";
        GoService client = new GoService(new RestTemplate());
        List<GoRelation> result = client.getChildren(goId);
        assertNotNull(result);
    }

    @Test
    void testRoot() {
        String goId = "";
        GoService client = new GoService(new RestTemplate());
        List<GoRelation> result = client.getChildren(goId);
        assertEquals(3, result.size());
    }
}
