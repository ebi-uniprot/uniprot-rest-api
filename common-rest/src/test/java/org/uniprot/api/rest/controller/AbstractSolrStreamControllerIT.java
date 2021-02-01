package org.uniprot.api.rest.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractSolrStreamControllerIT {
    protected static final String SAMPLE_RDF =
            "<?xml version='1.0' encoding='UTF-8'?>\n"
                    + "<rdf:RDF>\n"
                    + "    <owl:Ontology rdf:about=\"\">\n"
                    + "        <owl:imports rdf:resource=\"http://purl.uniprot.org/core/\"/>\n"
                    + "    </owl:Ontology>\n"
                    + "    <sample>text</sample>\n"
                    + "    <anotherSample>text2</anotherSample>\n"
                    + "    <someMore>text3</someMore>\n"
                    + "</rdf:RDF>";

    @RegisterExtension protected static DataStoreManager storeManager = new DataStoreManager();

    @Autowired protected MockMvc mockMvc;

    @Value("${search.default.page.size}")
    protected String defaultPageSize;

    @Value("${solr.query.batchSize}")
    protected String solrBatchSize;

    private int totalSavedEntries;

    @BeforeAll
    void initSolrAndInjectItInTheRepository() {
        storeManager.addSolrClient(getStoreType(), getSolrCollection());
        ReflectionTestUtils.setField(
                getRepository(), "solrClient", storeManager.getSolrClient(getStoreType()));
        totalSavedEntries = saveEntries();
        int minimalSavesEntries = (Integer.parseInt(defaultPageSize) * 2) + 1;

        assertThat(totalSavedEntries, greaterThan(5)); // 5 is the page size used in the tests
        assertThat(totalSavedEntries, greaterThan(minimalSavesEntries));
        assertThat(totalSavedEntries, greaterThan(Integer.parseInt(solrBatchSize)));
    }

    @AfterAll
    public void cleanData() {
        storeManager.cleanSolr(getStoreType());
    }

    protected abstract DataStoreManager.StoreType getStoreType();

    protected abstract SolrCollection getSolrCollection();

    protected abstract SolrQueryRepository getRepository();

    protected abstract int saveEntries();

    protected abstract String getStreamPath();

    @Test
    void streamCanReturnAll() throws Exception {

        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getStreamPath())
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "*:*");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(totalSavedEntries)));
    }

    @Test
    void streamBadRequest() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getStreamPath())
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "invalid:invalid")
                        .param("fields", "invalid,invalid1")
                        .param("sort", "invalid")
                        .param("download", "invalid");

        // then
        mockMvc.perform(requestBuilder)
                .andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                containsInAnyOrder(
                                        "'invalid' is not a valid search field",
                                        "Invalid fields parameter value 'invalid'",
                                        "Invalid fields parameter value 'invalid1'",
                                        "Invalid sort parameter format. Expected format fieldName asc|desc.",
                                        "The 'download' parameter has invalid format. It should be a boolean true or false.")));
    }

    @Test
    void searchSortWithIncorrectValuesReturnBadRequest() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getStreamPath())
                        .param("query", "*:*")
                        .param("sort", "invalidField desc,invalidField1 invalidSort1")
                        .header(ACCEPT, APPLICATION_JSON_VALUE);

        // then
        mockMvc.perform(requestBuilder)
                .andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                containsInAnyOrder(
                                        "Invalid sort field order 'invalidsort1'. Expected asc or desc",
                                        "Invalid sort field 'invalidfield1'",
                                        "Invalid sort field 'invalidfield'")));
    }

    // ------------------------------------------------------------
    //                       DOWNLOAD SCENARIOS TESTS
    // ------------------------------------------------------------

    @Test
    void streamWithDownloadFile() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getStreamPath())
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "*")
                        .param("download", "true");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(
                                        "Content-Disposition",
                                        startsWith(
                                                "form-data; name=\"attachment\"; filename=\"uniprot-")))
                .andExpect(header().string("Content-Disposition", not(endsWith(".gz"))))
                .andExpect(jsonPath("$.results.size()", is(totalSavedEntries)));
    }

    @Test
    void streamWithDownloadAndCompressedParamFile() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getStreamPath())
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "*")
                        .param("compressed", "true")
                        .param("download", "true");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        byte[] zippedResult =
                mockMvc.perform(asyncDispatch(response))
                        .andDo(log())
                        .andExpect(status().is(HttpStatus.OK.value()))
                        .andExpect(
                                header().string(
                                                "Content-Disposition",
                                                startsWith(
                                                        "form-data; name=\"attachment\"; filename=\"uniprot-")))
                        .andExpect(header().string("Content-Disposition", endsWith(".gz\"")))
                        .andReturn()
                        .getResponse()
                        .getContentAsByteArray();

        Assertions.assertNotNull(zippedResult);
        try (GZIPInputStream result = new GZIPInputStream(new ByteArrayInputStream(zippedResult))) {
            StringBuilder sb = new StringBuilder();
            final byte[] buffer = new byte[1024];
            while (result.read(buffer, 0, buffer.length) != -1) {
                sb.append(new String(buffer));
            }
            Assertions.assertNotNull(sb.toString());
            Assertions.assertTrue(sb.toString().startsWith("{\"results\":["));
        } catch (IOException io) {
            Assertions.fail(io);
        }
    }
}
