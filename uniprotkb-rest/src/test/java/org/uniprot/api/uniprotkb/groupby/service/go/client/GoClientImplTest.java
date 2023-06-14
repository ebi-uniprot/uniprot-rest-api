package org.uniprot.api.uniprotkb.groupby.service.go.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;

class GoClientImplTest {
    private GoClientImpl goClientImpl = new GoClientImpl();

    @Test
    void test() {
        String goId = "GO:0008150";
        goClientImpl = new GoClientImpl();
        List<GoRelation> result = goClientImpl.getChildren(goId);
        assertNotNull(result);
    }

    @Test
    void testRoot() {
        String goId = "";
        List<GoRelation> result = goClientImpl.getChildren(goId);
        assertEquals(3, result.size());
    }
}
