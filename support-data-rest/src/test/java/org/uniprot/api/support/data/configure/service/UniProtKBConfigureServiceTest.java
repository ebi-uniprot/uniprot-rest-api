package org.uniprot.api.support.data.configure.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import junit.framework.AssertionFailedError;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.uniprot.api.support.data.configure.response.AdvancedSearchTerm;

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
    void testGetUniProtSearchCrossReferences() {
        List<AdvancedSearchTerm> searchTerms = service.getUniProtSearchItems();
        assertNotNull(searchTerms);
        AdvancedSearchTerm crossRefGroup =
                searchTerms.stream()
                        .filter(term -> term.getId().equals("cross_references"))
                        .findFirst()
                        .orElseThrow(AssertionFailedError::new);
        assertNotNull(crossRefGroup);
        assertEquals("group", crossRefGroup.getItemType());
        assertNotNull(crossRefGroup.getItems());

        List<AdvancedSearchTerm> crossRefCat = crossRefGroup.getItems();
        assertEquals(20, crossRefCat.size());
        assertEquals("xref_group_any", crossRefCat.get(0).getId());

        AdvancedSearchTerm sequenceDatabasesCat =
                crossRefCat.stream()
                        .filter(term -> term.getId().equals("xref_group_sequence_databases"))
                        .findFirst()
                        .orElseThrow(AssertionFailedError::new);
        assertNotNull(sequenceDatabasesCat);
        assertEquals("group", sequenceDatabasesCat.getItemType());
        assertEquals("Sequence databases", sequenceDatabasesCat.getLabel());
        assertNotNull(sequenceDatabasesCat.getItems());

        AdvancedSearchTerm embl =
                sequenceDatabasesCat.getItems().stream()
                        .filter(term -> term.getId().equals("xref_embl"))
                        .findFirst()
                        .orElseThrow(AssertionFailedError::new);
        assertNotNull(embl);
        assertEquals("EMBL", embl.getLabel());
        assertEquals("single", embl.getItemType());
        assertEquals("xref", embl.getTerm());
        assertEquals("string", embl.getDataType());
        assertEquals("general", embl.getFieldType());
        assertEquals("embl-", embl.getValuePrefix());
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
