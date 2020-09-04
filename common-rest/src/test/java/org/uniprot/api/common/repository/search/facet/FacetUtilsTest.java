package org.uniprot.api.common.repository.search.facet;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * @author lgonzales
 * @since 01/09/2020
 */
class FacetUtilsTest {

    @Test
    void buildFacetItemsEmptyMap() {
        List<FacetItem> result = FacetUtils.buildFacetItems(Collections.emptyMap());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void buildFacetItemsSortedCountDesc() {
        Map<String, Long> items = new HashMap<>();
        items.put("The Count Value", 2L);
        items.put("The Count Value (Special chars)", 5L);
        items.put("The Count Value (Others chars)", 3L);
        List<FacetItem> result = FacetUtils.buildFacetItems(items);
        assertNotNull(result);
        assertEquals(3, result.size());

        FacetItem first = result.get(0);
        assertEquals("The Count Value (Special chars)", first.getLabel());
        assertEquals("the_count_value_special_chars", first.getValue());
        assertEquals(5L, first.getCount());

        FacetItem second = result.get(1);
        assertEquals("The Count Value (Others chars)", second.getLabel());
        assertEquals("the_count_value_others_chars", second.getValue());
        assertEquals(3L, second.getCount());

        FacetItem last = result.get(2);
        assertEquals("The Count Value", last.getLabel());
        assertEquals("the_count_value", last.getValue());
        assertEquals(2L, last.getCount());
    }

    @Test
    void getCleanFacetSimpleValueLowerCase() {
        String cleanValue = FacetUtils.getCleanFacetValue("Value");
        assertEquals("value", cleanValue);
    }

    @Test
    void getCleanFacetSpecialValue() {
        String cleanValue = FacetUtils.getCleanFacetValue("(Special chars)");
        assertEquals("special_chars", cleanValue);
    }
}
