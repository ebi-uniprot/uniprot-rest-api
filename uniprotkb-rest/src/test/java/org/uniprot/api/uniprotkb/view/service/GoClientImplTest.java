package org.uniprot.api.uniprotkb.view.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.uniprot.api.uniprotkb.view.GoRelation;

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
