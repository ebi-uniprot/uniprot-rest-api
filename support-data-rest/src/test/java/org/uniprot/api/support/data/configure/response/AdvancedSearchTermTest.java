package org.uniprot.api.support.data.configure.response;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;

import junit.framework.AssertionFailedError;

class AdvancedSearchTermTest {

    private static SearchFieldConfig fieldConfig;

    @BeforeAll
    static void setUp() {
        fieldConfig = SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIPROTKB);
    }

    @Test
    void testGetTopLevelFields() {
        List<SearchFieldItem> roots = AdvancedSearchTerm.getTopLevelFieldItems(fieldConfig);
        assertNotNull(roots);
        assertFalse(roots.isEmpty());
    }

    @Test
    void testGetChildFields() {
        String parentId = "structure";
        List<SearchFieldItem> fields = AdvancedSearchTerm.getChildFieldItems(fieldConfig, parentId);
        assertNotNull(fields);
        assertFalse(fields.isEmpty());
    }

    @Test
    void testGetNoChildFields() {
        String parentId = "length_sort";
        List<SearchFieldItem> fields = AdvancedSearchTerm.getChildFieldItems(fieldConfig, parentId);
        assertNotNull(fields);
        assertTrue(fields.isEmpty());
    }

    @Test
    void testGetChildFieldsWithWrongParent() {
        String parentId = "random";
        List<SearchFieldItem> fields = AdvancedSearchTerm.getChildFieldItems(fieldConfig, parentId);
        assertNotNull(fields);
        assertTrue(fields.isEmpty());
    }

    @Test
    void testGetChildFieldsWithTags() {
        String parentId = "function";
        List<SearchFieldItem> fields = AdvancedSearchTerm.getChildFieldItems(fieldConfig, parentId);
        assertNotNull(fields);
        assertFalse(fields.isEmpty());
        SearchFieldItem catalyticItem =
                fields.stream()
                        .filter(field -> field.getId().equals("catalytic_activity"))
                        .findFirst()
                        .orElseThrow(AssertionFailedError::new);
        assertNotNull(catalyticItem);
        assertNotNull(catalyticItem.getTags());
        assertFalse(catalyticItem.getTags().isEmpty());
        assertTrue(catalyticItem.getTags().contains("CHEBI"));
    }
}
