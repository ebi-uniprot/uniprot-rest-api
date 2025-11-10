package org.uniprot.api.idmapping.controller;

import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.core.Is.is;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.uniprot.api.idmapping.common.JobOperation;
import org.uniprot.api.idmapping.common.model.IdMappingJob;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.document.Document;

public abstract class IdMappingGroupByControllerIT {
    protected static final String FREE_FORM_QUERY = "free-form-query";

    @Value("${mapping.max.to.ids.with.facets.count}")
    protected Integer maxToIdsWithFacetsAllowed;

    @Autowired protected JobOperation idMappingResultJobOp;
    private static final String INVALID_ORGANISM_ID = "36";
    protected static final String CHEBI_ID = "12345678";

    @Test
    void getGroupBy_emptyResults() throws Exception {
        prepareSingleRootNodeWithNoChildren();
        IdMappingJob job =
                idMappingResultJobOp.createAndPutJobInCache(
                        this.maxToIdsWithFacetsAllowed - 1, JobStatus.FINISHED);

        getMockMvc()
                .perform(
                        MockMvcRequestBuilders.get(getUrlWithJobId(job.getJobId()))
                                .param("query", "organism_id:" + INVALID_ORGANISM_ID))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups.size()", is(0)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors.size()", is(0)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.label").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.count", is(0)));
    }

    protected String getUrlWithJobId(String jobId) {
        return String.format(getPath(), jobId);
    }

    @Test
    void getGroupBy_whenFreeFormQueryAndEmptyResults() throws Exception {
        prepareSingleRootNodeWithNoChildren();
        IdMappingJob job =
                idMappingResultJobOp.createAndPutJobInCache(
                        this.maxToIdsWithFacetsAllowed - 1, JobStatus.FINISHED);

        getMockMvc()
                .perform(
                        MockMvcRequestBuilders.get(getUrlWithJobId(job.getJobId()))
                                .param("query", FREE_FORM_QUERY))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups.size()", is(0)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors.size()", is(0)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.label").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.count", is(0)));
    }

    @Test
    void getGroupBy_withEmptyListOfToIds() throws Exception {
        prepareSingleRootNodeWithNoChildren();
        IdMappingJob job = idMappingResultJobOp.createAndPutJobInCache(0, JobStatus.FINISHED);

        getMockMvc()
                .perform(
                        MockMvcRequestBuilders.get(getUrlWithJobId(job.getJobId()))
                                .param("query", FREE_FORM_QUERY))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.jsonPath("$.groups.size()", is(0)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ancestors.size()", is(0)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.label").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent.count", is(0)));
    }

    @Test
    void getGroupBy_invalidParent() throws Exception {
        IdMappingJob job =
                idMappingResultJobOp.createAndPutJobInCache(
                        this.maxToIdsWithFacetsAllowed - 1, JobStatus.FINISHED);
        getMockMvc()
                .perform(
                        MockMvcRequestBuilders.get(getUrlWithJobId(job.getJobId()))
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
    void getGroupBy_invalidJobId() throws Exception {
        getMockMvc()
                .perform(MockMvcRequestBuilders.get(getUrlWithJobId("12345")).param("query", "*"))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    void getGroupBy_unfinishedJob() throws Exception {
        IdMappingJob job =
                idMappingResultJobOp.createAndPutJobInCache(
                        this.maxToIdsWithFacetsAllowed - 1, JobStatus.RUNNING);
        getMockMvc()
                .perform(
                        MockMvcRequestBuilders.get(getUrlWithJobId(job.getJobId()))
                                .param("query", "*"))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    void getGroupBy_toIdsExceedingFacets() throws Exception {
        IdMappingJob job =
                idMappingResultJobOp.createAndPutJobInCache(
                        this.maxToIdsWithFacetsAllowed + 1, JobStatus.FINISHED);
        getMockMvc()
                .perform(
                        MockMvcRequestBuilders.get(getUrlWithJobId(job.getJobId()))
                                .param("query", "*"))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(
                        MockMvcResultMatchers.content()
                                .string(containsStringIgnoringCase("filters are not supported")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.parent").doesNotExist());
    }

    @Test
    void getGroupBy_chebiUppercaseQuery() throws Exception {
        prepareSingleRootNodeWithNoChildren();
        IdMappingJob job =
                idMappingResultJobOp.createAndPutJobInCache(
                        this.maxToIdsWithFacetsAllowed - 1, JobStatus.FINISHED);

        getMockMvc()
                .perform(
                        MockMvcRequestBuilders.get(getUrlWithJobId(job.getJobId()))
                                .param("query", "CHEBI:" + CHEBI_ID))
                .andDo(MockMvcResultHandlers.print())
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
