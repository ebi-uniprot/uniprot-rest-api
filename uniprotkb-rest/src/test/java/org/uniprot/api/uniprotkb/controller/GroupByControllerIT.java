package org.uniprot.api.uniprotkb.controller;

import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.core.Is.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.document.Document;

public abstract class GroupByControllerIT {
    protected static final String EMPTY_PARENT = "";
    private static final String INVALID_ORGANISM_ID = "36";

    @Test
    void getGroupByKeyword_emptyResults() throws Exception {
        prepareSingleRootNodeWithNoChildren();

        getMockMvc()
                .perform(
                        get(getPath())
                                .param("query", "organism_id:" + INVALID_ORGANISM_ID)
                                .param("parent", EMPTY_PARENT))
                .andDo(log())
                .andExpect(jsonPath("$.groups.size()", is(0)))
                .andExpect(jsonPath("$.ancestors.size()", is(0)))
                .andExpect(jsonPath("$.parent").doesNotExist());
    }

    @Test
    void getGroupByKeyword_whenFreeFormQueryAndEmptyResults() throws Exception {
        prepareSingleRootNodeWithNoChildren();

        getMockMvc()
                .perform(
                        get(getPath())
                                .param("query", INVALID_ORGANISM_ID)
                                .param("parent", EMPTY_PARENT))
                .andDo(log())
                .andExpect(jsonPath("$.groups.size()", is(0)))
                .andExpect(jsonPath("$.ancestors.size()", is(0)))
                .andExpect(jsonPath("$.parent").doesNotExist());
    }

    @Test
    void getGroupByKeyword_whenQueryNotSpecified() throws Exception {
        getMockMvc()
                .perform(get(getPath()).param("parent", EMPTY_PARENT))
                .andDo(log())
                .andExpect(status().isBadRequest())
                .andExpect(
                        content()
                                .string(
                                        containsStringIgnoringCase(
                                                "query is a required parameter")))
                .andExpect(jsonPath("$.parent").doesNotExist());
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
