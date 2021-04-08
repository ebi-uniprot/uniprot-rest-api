package org.uniprot.api.rest.request;

import static org.junit.jupiter.api.Assertions.*;
import static org.uniprot.api.rest.request.MetaReaderUtil.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * @author lgonzales
 * @since 18/05/2020
 */
class QueryFieldMetaReaderImplTest {

    @Test
    void readInvalidFile() {
        QueryFieldMetaReaderImpl queryFieldMetaReaderImpl = new QueryFieldMetaReaderImpl();
        assertThrows(
                IllegalArgumentException.class,
                () -> queryFieldMetaReaderImpl.read("invalid-invalid.json"));
    }

    @Test
    void readCrossReference() {
        QueryFieldMetaReaderImpl queryFieldMetaReaderImpl = new QueryFieldMetaReaderImpl();
        List<Map<String, Object>> result =
                queryFieldMetaReaderImpl.read("crossref-search-fields.json");
        assertNotNull(result);

        assertEquals(3, result.size());
        assertTrue(validateFieldMap(result, "name", "category_str"));
        assertTrue(validateFieldMap(result, "name", "id"));
        assertTrue(validateFieldMap(result, "name", "name"));

        assertFalse(validateFieldMap(result, "name", "content"));

        assertTrue(
                validateFieldMap(
                        result, "description", "Search cross-reference by id which is accession"));
        assertTrue(validateFieldMap(result, "dataType", "string"));
        assertTrue(validateFieldMap(result, "example", "DB-0236"));
        assertTrue(validateFieldMap(result, "regex", "^DB-[0-9]{4}$"));
    }
}
