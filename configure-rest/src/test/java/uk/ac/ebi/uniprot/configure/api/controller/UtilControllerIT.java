package uk.ac.ebi.uniprot.configure.api.controller;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import uk.ac.ebi.uniprot.api.configure.ConfigureApplication;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 *
 * @author lgonzales
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {ConfigureApplication.class})
@WebAppConfiguration
public class UtilControllerIT {

    private static final String PARSE_QUERY_RESOURCE = "/util/queryParser";
    private static final String QUERY_PARAM = "query";

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.
                webAppContextSetup(webApplicationContext)
                .build();
    }


    @Test
    public void validQueryRequest() throws Exception {

        // when
        ResultActions response = mockMvc.perform(
                get(PARSE_QUERY_RESOURCE)
                        .param(QUERY_PARAM,"accession:P21802")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.type",is("termQuery")))
                .andExpect(jsonPath("$.field",is("accession")))
                .andExpect(jsonPath("$.value",is("p21802")));
    }

    @Test
    public void requiredQueryRequest() throws Exception {

        // when
        ResultActions response = mockMvc.perform(
                get(PARSE_QUERY_RESOURCE)
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.messages.*",contains("query is a required parameter")));
    }

    @Test
    public void invalidQueryRequest() throws Exception {

        // when
        ResultActions response = mockMvc.perform(
                get(PARSE_QUERY_RESOURCE)
                        .param(QUERY_PARAM,"length:[1 TO ]")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.messages.*",contains("query parameter has an invalid syntax")));
    }

}