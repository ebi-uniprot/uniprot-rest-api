package org.uniprot.api.support.data.configure.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.uniprot.api.support.data.configure.service.UtilServiceTest.CONTEXT_PATH;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.uniprot.api.support.data.configure.response.AdvancedSearchTerm;
import org.uniprot.api.support.data.configure.response.UniProtReturnField;

import junit.framework.AssertionFailedError;

class UniProtKBConfigureServiceTest {
    private static UniProtKBConfigureService service;

    @BeforeAll
    static void initAll() {
        service = new UniProtKBConfigureService();
    }

    @Test
    void testGetUniProtSearchItems() {
        List<AdvancedSearchTerm> items = service.getUniProtSearchItems(CONTEXT_PATH);
        assertEquals(31, items.size());
    }

    @Test
    void testGetUniProtSearchCrossReferences() {
        List<AdvancedSearchTerm> searchTerms = service.getUniProtSearchItems(CONTEXT_PATH);
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
        assertTrue(crossRefCat.stream().anyMatch(e -> !e.getId().equals("xref_group_ontologies")));
        assertEquals(21, crossRefCat.size());
        AdvancedSearchTerm sourceItem = crossRefCat.get(0);
        assertEquals("xref_source", sourceItem.getId());
        assertEquals("source", sourceItem.getTerm());
        AdvancedSearchTerm anyGroup = crossRefCat.get(1);
        assertEquals("xref_group_any", anyGroup.getId());
        assertEquals(1, anyGroup.getItems().size());
        assertEquals("xref_any", anyGroup.getItems().get(0).getId());
        assertNull(anyGroup.getItems().get(0).getValuePrefix());

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
    void testGetResultFields2() {
        List<UniProtReturnField> groups = service.getResultFields2();
        assertEquals(31, groups.size());

        List<UniProtReturnField> fields =
                groups.stream()
                        .flatMap(group -> group.getFields().stream())
                        .collect(Collectors.toList());
        assertNotNull(fields);
        assertFalse(fields.isEmpty());

        List<UniProtReturnField> multiValuesXrefs =
                fields.stream()
                        .filter(field -> field.getIsMultiValueCrossReference() != null)
                        .filter(UniProtReturnField::getIsMultiValueCrossReference)
                        .collect(Collectors.toList());
        assertNotNull(multiValuesXrefs);

        assertEquals(87, multiValuesXrefs.size());
        for (UniProtReturnField xref : multiValuesXrefs) {
            assertTrue(xref.getName().startsWith("xref_"));
        }
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
