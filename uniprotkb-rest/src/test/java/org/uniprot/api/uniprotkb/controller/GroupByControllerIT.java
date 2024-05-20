package org.uniprot.api.uniprotkb.controller;

import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.core.Is.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.document.Document;

public abstract class GroupByControllerIT {
    private static final String INVALID_ORGANISM_ID = "36";
    protected static final String CHEBI_ID = "12345678";

    @Test
    void getGroupBy_emptyResults() throws Exception {
        prepareSingleRootNodeWithNoChildren();

        getMockMvc()
                .perform(
                        MockMvcRequestBuilders.get(getPath())
                                .param("query", "organism_id:" + INVALID_ORGANISM_ID))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups.size()", is(0)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors.size()", is(0)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.label").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.count", is(0)));
    }

    @Test
    void getGroupBy_whenFreeFormQueryAndEmptyResults() throws Exception {
        prepareSingleRootNodeWithNoChildren();

        getMockMvc()
                .perform(MockMvcRequestBuilders.get(getPath()).param("query", INVALID_ORGANISM_ID))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups.size()", is(0)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors.size()", is(0)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.label").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.count", is(0)));
    }

    @Test
    void getGroupBy_whenQueryNotSpecified() throws Exception {
        getMockMvc()
                .perform(MockMvcRequestBuilders.get(getPath()))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(
                        MockMvcResultMatchers.content()
                                .string(
                                        containsStringIgnoringCase(
                                                "query is a required parameter")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent").doesNotExist());
    }

    @Test
    void getGroupBy_invalidParent() throws Exception {
        getMockMvc()
                .perform(
                        MockMvcRequestBuilders.get(getPath())
                                .param("query", "*")
                                .param("parent", "invalid-parent"))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(
                        MockMvcResultMatchers.content()
                                .string(containsStringIgnoringCase("id value should be")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent").doesNotExist());
    }

    @Test
    void getGroupBy_ChebiUppercaseQuery() throws Exception {
        prepareSingleRootNodeWithNoChildren();

        getMockMvc()
                .perform(MockMvcRequestBuilders.get(getPath()).param("query", "CHEBI:" + CHEBI_ID))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups.size()", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.count", is(1)));
    }

    protected void save(DataStoreManager.StoreType type, Document doc) {
        getDataStoreManager().saveDocs(type, doc);
    }

    protected void save(Document doc) {
        getDataStoreManager().saveDocs(DataStoreManager.StoreType.UNIPROT, doc);
    }

    protected abstract DataStoreManager getDataStoreManager();

    protected abstract MockMvc getMockMvc();

    protected abstract String getPath();

    protected abstract void prepareSingleRootNodeWithNoChildren() throws Exception;
}
