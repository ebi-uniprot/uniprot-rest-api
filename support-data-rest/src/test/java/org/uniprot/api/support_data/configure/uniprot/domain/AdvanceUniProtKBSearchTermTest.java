package org.uniprot.api.support_data.configure.uniprot.domain;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.uniprot.api.support_data.configure.uniprot.domain.model.AdvanceUniProtKBSearchTerm;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;

class AdvanceUniProtKBSearchTermTest {

    private static SearchFieldConfig fieldConfig;

    @BeforeAll
    static void setUp() {
        fieldConfig = SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIPROTKB);
    }

    @Test
    void testGetTopLevelFields() {
        List<SearchFieldItem> roots = AdvanceUniProtKBSearchTerm.getTopLevelFieldItems(fieldConfig);
        Assertions.assertNotNull(roots);
        Assertions.assertFalse(roots.isEmpty());
    }

    @Test
    void testGetChildFields() {
        String parentId = "structure";
        List<SearchFieldItem> fields =
                AdvanceUniProtKBSearchTerm.getChildFieldItems(fieldConfig, parentId);
        Assertions.assertNotNull(fields);
        Assertions.assertFalse(fields.isEmpty());
    }

    @Test
    void testGetNoChildFields() {
        String parentId = "length_sort";
        List<SearchFieldItem> fields =
                AdvanceUniProtKBSearchTerm.getChildFieldItems(fieldConfig, parentId);
        Assertions.assertNotNull(fields);
        Assertions.assertTrue(fields.isEmpty());
    }

    @Test
    void testGetChildFieldsWithWrongParent() {
        String parentId = "random";
        List<SearchFieldItem> fields =
                AdvanceUniProtKBSearchTerm.getChildFieldItems(fieldConfig, parentId);
        Assertions.assertNotNull(fields);
        Assertions.assertTrue(fields.isEmpty());
    }
}
