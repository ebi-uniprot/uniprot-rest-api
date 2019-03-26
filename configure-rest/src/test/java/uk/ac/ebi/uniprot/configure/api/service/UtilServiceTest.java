package uk.ac.ebi.uniprot.configure.api.service;

import org.junit.jupiter.api.Test;
import uk.ac.ebi.uniprot.configure.uniprot.domain.query.SolrJsonQuery;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author lgonzales
 */
class UtilServiceTest {

    private UtilService service = new UtilService();

    @Test
    void convertValidQuery() {
        SolrJsonQuery jsonQuery = service.convertQuery("field:value");
        assertNotNull(jsonQuery);
        assertEquals("termQuery",jsonQuery.getType());
        assertEquals("field",jsonQuery.getField());
        assertEquals("value",jsonQuery.getValue());
    }

    @Test
    void convertEmptyQuery() {
        RuntimeException thrown =
                assertThrows(RuntimeException.class,
                        () ->  service.convertQuery(""));
        assertEquals("Query is required",thrown.getMessage());

    }

    @Test
    void convertInvalidQuery() {

        RuntimeException thrown =
                assertThrows(RuntimeException.class,
                        () ->  service.convertQuery("field:[ TO 20]"));
        assertEquals("Invalid query requested: field:[ TO 20]",thrown.getMessage());

    }

}