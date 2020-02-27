package org.uniprot.api.configure.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.uniprot.api.configure.uniprot.domain.model.AdvanceUniProtKBSearchTerm;

class UniProtConfigureServiceTest {
    private static UniProtConfigureService service;

    @BeforeAll
    static void initAll() {
        service = new UniProtConfigureService();
    }

    @Test
    void testGetUniProtSearchItems() {
        List<AdvanceUniProtKBSearchTerm> items = service.getUniProtSearchItems();
        assertEquals(27, items.size());
    }

    @Test
    void testGetAnnotationEvidences() {
        assertEquals(3, service.getAnnotationEvidences().size());
    }

    @Test
    void testGetGoEvidences() {
        assertEquals(3, service.getGoEvidences().size());
    }

    @Test
    void testGetDatabases() {
        assertTrue(service.getDatabases().size() >= 19);
    }
}
