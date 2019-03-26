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
import uk.ac.ebi.uniprot.configure.api.ConfigureApplication;

import static org.hamcrest.Matchers.greaterThan;
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
public class UniProtConfigureControllerIT {

    private static final String BASIC_RESOURCE = "/uniprotkb";

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
    public void canGetUniProtSearchTerms() throws Exception {

        // when
        ResultActions response = mockMvc.perform(
                get(BASIC_RESOURCE+"/search_terms")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        validateResponse(response);
    }


    @Test
    public void canGetAnnotationEvidences() throws Exception {

        // when
        ResultActions response = mockMvc.perform(
                get(BASIC_RESOURCE+"/annotation_evidences")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        validateResponse(response);
    }

    @Test
    public void canGetGoEvidences() throws Exception {

        // when
        ResultActions response = mockMvc.perform(
                get(BASIC_RESOURCE+"/go_evidences")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        validateResponse(response);
    }

    @Test
    public void canGetDatabases() throws Exception {

        // when
        ResultActions response = mockMvc.perform(
                get(BASIC_RESOURCE+"/databases")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        validateResponse(response);
    }

    @Test
    public void canGetDatabasesFields() throws Exception {

        // when
        ResultActions response = mockMvc.perform(
                get(BASIC_RESOURCE+"/databasefields")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        validateResponse(response);
    }

    @Test
    public void canGetResultFields() throws Exception {

        // when
        ResultActions response = mockMvc.perform(
                get(BASIC_RESOURCE+"/resultfields")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        validateResponse(response);
    }

    private void validateResponse(ResultActions response) throws Exception {
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.size()", is(greaterThan(0))));
    }

}