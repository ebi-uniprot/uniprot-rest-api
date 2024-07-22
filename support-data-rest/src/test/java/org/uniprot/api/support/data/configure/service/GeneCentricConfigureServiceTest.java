package org.uniprot.api.support.data.configure.service;

import org.junit.jupiter.api.Test;
import org.uniprot.api.support.data.configure.response.AdvancedSearchTerm;
import org.uniprot.api.support.data.configure.response.UniProtReturnField;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.uniprot.api.support.data.configure.service.UtilServiceTest.CONTEXT_PATH;

class GeneCentricConfigureServiceTest {

    @Test
    void getResultFields() {
        GeneCentricConfigureService service = new GeneCentricConfigureService();
        List<UniProtReturnField> resultGroups = service.getResultFields();

        assertNotNull(resultGroups);
        assertEquals(1, resultGroups.size());

        assertEquals(6, resultGroups.get(0).getFields().size());
    }

    @Test
    void getSearchItems() {
        GeneCentricConfigureService service = new GeneCentricConfigureService();
        List<AdvancedSearchTerm> result = service.getSearchItems(CONTEXT_PATH);
        assertNotNull(result);
        assertEquals(4, result.size());
    }
}