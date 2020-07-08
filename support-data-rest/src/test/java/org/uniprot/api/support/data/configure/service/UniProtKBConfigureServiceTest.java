package org.uniprot.api.support.data.configure.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.uniprot.api.support.data.configure.domain.AdvancedSearchTerm;

class UniProtKBConfigureServiceTest {
    private static UniProtKBConfigureService service;

    @BeforeAll
    static void initAll() {
        service = new UniProtKBConfigureService();
    }

    @Test
    void testGetUniProtSearchItems() {
        List<AdvancedSearchTerm> items = service.getUniProtSearchItems();
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

    @Test
    void testGetEvidenceDatabases() {
        assertTrue(service.getEvidenceDatabases().size() >= 45);
    }
}
