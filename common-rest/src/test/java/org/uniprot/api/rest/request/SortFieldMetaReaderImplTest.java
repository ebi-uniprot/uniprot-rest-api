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
class SortFieldMetaReaderImplTest {

    @Test
    void readInvalidFileName() {
        SortFieldMetaReaderImpl sortFieldMetaReaderImpl = new SortFieldMetaReaderImpl();
        assertThrows(
                IllegalArgumentException.class,
                () -> sortFieldMetaReaderImpl.read("invalid-invalid.json"));
    }

    @Test
    void testReadSortFieldsForCrossReference() {
        SortFieldMetaReaderImpl sortFieldMetaReaderImpl = new SortFieldMetaReaderImpl();
        List<Map<String, Object>> result =
                sortFieldMetaReaderImpl.read("crossref-search-fields.json");
        assertNotNull(result);

        assertEquals(2, result.size());
        assertTrue(validateFieldMap(result, "name", "category_str"));
        assertTrue(validateFieldMap(result, "example", "category_str asc"));

        assertFalse(validateFieldMap(result, "name", "content"));
    }
}
