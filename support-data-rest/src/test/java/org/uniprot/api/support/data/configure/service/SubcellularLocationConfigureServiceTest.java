package org.uniprot.api.support.data.configure.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.uniprot.api.support.data.configure.response.AdvancedSearchTerm;
import org.uniprot.api.support.data.configure.response.UniProtReturnField;

/**
 * @author lgonzales
 * @since 18/03/2021
 */
class SubcellularLocationConfigureServiceTest {

    @Test
    void getResultFields() {
        SubcellularLocationConfigureService service = new SubcellularLocationConfigureService();
        List<UniProtReturnField> resultGroups = service.getResultFields();

        assertNotNull(resultGroups);
        assertEquals(1, resultGroups.size());

        assertEquals(14, resultGroups.get(0).getFields().size());
    }

    @Test
    void getSearchItems() {
        SubcellularLocationConfigureService service = new SubcellularLocationConfigureService();
        List<AdvancedSearchTerm> result = service.getSearchItems();
        assertNotNull(result);
        assertEquals(2, result.size());
    }
}
