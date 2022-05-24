package org.uniprot.api.support.data.suggester.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.uniprot.store.search.document.suggest.SuggestDictionary.TAXONOMY;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.request.json.JsonQueryRequest;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.api.support.data.DataStoreTestConfig;
import org.uniprot.api.support.data.SupportDataRestApplication;

/**
 * Created 19/05/19
 *
 * @author Edd
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(
        classes = {
            SuggesterControllerWithServerErrorsIT.OtherConfig.class,
            DataStoreTestConfig.class,
            SupportDataRestApplication.class
        })
@WebAppConfiguration
@ActiveProfiles({"server-errors"})
class SuggesterControllerWithServerErrorsIT {
    private static final String SEARCH_RESOURCE = "/suggester";

    @Autowired private SolrClient solrClient;

    @Autowired private WebApplicationContext webApplicationContext;

    @Autowired
    @Qualifier("xrefRDFRestTemplate")
    private RestTemplate xrefRestTemplate;

    @Autowired
    @Qualifier("diseaseRDFRestTemplate")
    private RestTemplate diseaseRestTemplate;

    @Autowired
    @Qualifier("keywordRDFRestTemplate")
    private RestTemplate keywordRDFRestTemplate;

    @Autowired
    @Qualifier("taxonomyRDFRestTemplate")
    private RestTemplate taxonomyRDFRestTemplate;

    @Autowired
    @Qualifier("locationRDFRestTemplate")
    private RestTemplate locationRDFRestTemplate;

    @Autowired
    @Qualifier("literatureRDFRestTemplate")
    private RestTemplate literatureRDFRestTemplate;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void solrErrorCauses500() throws Exception {
        // given
        doThrow(IllegalStateException.class)
                .when(solrClient)
                .query(anyString(), any(SolrQuery.class));

        // when
        ResultActions response =
                mockMvc.perform(
                        get(SEARCH_RESOURCE)
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param("query", "anything")
                                .param("dict", TAXONOMY.name()));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.INTERNAL_SERVER_ERROR.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE));
    }

    @TestConfiguration
    @Profile("server-errors")
    static class OtherConfig {
        @Bean
        @Primary
        public SolrClient solrClient() {
            return mock(SolrClient.class);
        }

        @Bean
        @Primary
        public SolrRequestConverter requestConverter() {
            return new SolrRequestConverter() {
                @Override
                public JsonQueryRequest toJsonQueryRequest(SolrRequest request) {
                    JsonQueryRequest solrQuery = super.toJsonQueryRequest(request);

                    // required for tests, because EmbeddedSolrServer is not sharded
                    ((ModifiableSolrParams) solrQuery.getParams()).set("distrib", "false");
                    ((ModifiableSolrParams) solrQuery.getParams()).set("terms.mincount", "1");

                    return solrQuery;
                }
            };
        }

        @Bean(name = "xrefRDFRestTemplate")
        public RestTemplate xrefRestTemplate() {
            return mock(RestTemplate.class);
        }

        @Bean(name = "diseaseRDFRestTemplate")
        public RestTemplate diseaseRestTemplate() {
            return mock(RestTemplate.class);
        }

        @Bean(name = "keywordRDFRestTemplate")
        public RestTemplate keywordRDFRestTemplate() {
            return mock(RestTemplate.class);
        }

        @Bean(name = "taxonomyRDFRestTemplate")
        public RestTemplate taxonomyRDFRestTemplate() {
            return mock(RestTemplate.class);
        }

        @Bean(name = "locationRDFRestTemplate")
        public RestTemplate locationRDFRestTemplate() {
            return mock(RestTemplate.class);
        }

        @Bean(name = "literatureRDFRestTemplate")
        public RestTemplate literaturesRDFRestTemplate() {
            return mock(RestTemplate.class);
        }
    }
}
