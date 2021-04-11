package org.uniprot.api.support.data.configure.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.uniprot.api.support.data.configure.response.AdvancedSearchTerm;
import org.uniprot.api.support.data.configure.response.UniProtReturnField;

/**
 * @author lgonzales
 * @since 11/04/2021
 */
class LiteratureConfigureServiceTest {

    @Test
    void getResultFields() {
        LiteratureConfigureService service = new LiteratureConfigureService();
        List<UniProtReturnField> resultGroups = service.getResultFields();

        assertNotNull(resultGroups);
        assertEquals(1, resultGroups.size());

        assertEquals(13, resultGroups.get(0).getFields().size());
    }

    @Test
    void getSearchItems() {
        LiteratureConfigureService service = new LiteratureConfigureService();
        List<AdvancedSearchTerm> result = service.getSearchItems();
        assertNotNull(result);
        assertEquals(6, result.size());
    }
}
