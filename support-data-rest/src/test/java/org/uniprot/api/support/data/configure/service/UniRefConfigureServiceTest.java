package org.uniprot.api.support.data.configure.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.uniprot.api.support.data.configure.response.AdvancedSearchTerm;
import org.uniprot.api.support.data.configure.response.UniProtReturnField;

/**
 * @author lgonzales
 * @since 27/05/2020
 */
class UniRefConfigureServiceTest {

    @Test
    void getResultFields() {
        UniRefConfigureService service = new UniRefConfigureService();
        List<UniProtReturnField> resultGroups = service.getResultFields();

        assertNotNull(resultGroups);
        assertEquals(4, resultGroups.size());

        assertEquals(6, resultGroups.get(0).getFields().size());
        assertEquals(3, resultGroups.get(1).getFields().size());
        assertEquals(3, resultGroups.get(2).getFields().size());
        assertEquals(1, resultGroups.get(3).getFields().size());
    }

    @Test
    void getSearchItems() {
        UniRefConfigureService service = new UniRefConfigureService();
        List<AdvancedSearchTerm> result = service.getSearchItems();
        assertNotNull(result);
        assertEquals(9, result.size());
    }
}
