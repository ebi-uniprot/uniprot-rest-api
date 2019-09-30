package org.uniprot.api.uniprotkb.view.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.uniprotkb.view.GoTerm;

class GoServiceTest {

    @Test
    void test() {
        String goId = "GO:0008150";
        GoService client = new GoService(new RestTemplate());
        Optional<GoTerm> result = client.getChildren(goId);
        assertTrue(result.isPresent());
        assertNotNull(result.get());
    }

    @Test
    void testRoot() {
        String goId = "";
        GoService client = new GoService(new RestTemplate());
        Optional<GoTerm> result = client.getChildren(goId);
        assertTrue(result.isPresent());
        assertEquals(3, result.get().getChildren().size());
    }
}
