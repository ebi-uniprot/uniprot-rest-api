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
class DiseaseConfigureServiceTest {

    @Test
    void getResultFields() {
        DiseaseConfigureService service = new DiseaseConfigureService();
        List<UniProtReturnField> resultGroups = service.getResultFields();

        assertNotNull(resultGroups);
        assertEquals(1, resultGroups.size());

        assertEquals(9, resultGroups.get(0).getFields().size());
    }

    @Test
    void getSearchItems() {
        DiseaseConfigureService service = new DiseaseConfigureService();
        List<AdvancedSearchTerm> result = service.getSearchItems();
        assertNotNull(result);
        assertEquals(2, result.size());
    }
}
