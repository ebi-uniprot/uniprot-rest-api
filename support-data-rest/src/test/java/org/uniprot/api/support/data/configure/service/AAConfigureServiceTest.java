package org.uniprot.api.support.data.configure.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.uniprot.api.support.data.configure.service.UtilServiceTest.CONTEXT_PATH;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.uniprot.api.support.data.configure.response.AdvancedSearchTerm;
import org.uniprot.api.support.data.configure.response.UniProtReturnField;

/**
 * @author sahmad
 * @created 29/07/2021
 */
class AAConfigureServiceTest {
    private static AAConfigureService SERVICE;

    @BeforeAll
    static void init() {
        SERVICE = new AAConfigureService();
    }

    @Test
    void getUniRuleResultFields() {
        List<UniProtReturnField> resultGroups = SERVICE.getUniRuleResultFields();

        assertNotNull(resultGroups);
        assertNotNull(resultGroups);
        assertEquals(1, resultGroups.size());

        assertEquals(7, resultGroups.get(0).getFields().size());
    }

    @Test
    void getUniRuleSearchItems() {
        List<AdvancedSearchTerm> result = SERVICE.getUniRuleSearchItems(CONTEXT_PATH);
        assertNotNull(result);
        assertEquals(10, result.size());
    }

    @Test
    void getArbaResultFields() {
        List<UniProtReturnField> resultGroups = SERVICE.getArbaResultFields();

        assertNotNull(resultGroups);
        assertNotNull(resultGroups);
        assertEquals(1, resultGroups.size());

        assertEquals(5, resultGroups.get(0).getFields().size());
    }

    @Test
    void getArbaSearchItems() {
        List<AdvancedSearchTerm> result = SERVICE.getArbaSearchItems(CONTEXT_PATH);
        assertNotNull(result);
        assertEquals(7, result.size());
    }
}
