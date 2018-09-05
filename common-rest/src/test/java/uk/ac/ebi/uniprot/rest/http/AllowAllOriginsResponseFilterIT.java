package uk.ac.ebi.uniprot.rest.http;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.ac.ebi.uniprot.rest.http.app.FakeRESTApp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.ac.ebi.uniprot.rest.http.HttpCommonHeaderConfig.ALLOW_ALL_ORIGINS;
import static uk.ac.ebi.uniprot.rest.http.app.FakeRESTApp.RESOURCE_1_URL;

/**
 * Check that a REST app that picks up an {@link HttpCommonHeaderConfig} will show explicitly
 * the all origins header value, '*' -- and not the actual origin.
 *
 * Created 13/03/18
 * @author Edd
 */
@ActiveProfiles("allow-origins-integration-test")
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {FakeRESTApp.class})
@WebAppConfiguration
public class AllowAllOriginsResponseFilterIT {
    @Autowired
    private WebApplicationContext webApplicationContext;
    private MockMvc mockMvc;
    @Autowired
    private OncePerRequestFilter originsFilter;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .addFilter(originsFilter)
                .build();
    }

    @Test
    public void requestWithoutAnOriginHasResponseWithAllOriginsHeader() throws Exception {
        MvcResult result = mockMvc.perform(
                get(RESOURCE_1_URL))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getHeader(ACCESS_CONTROL_ALLOW_ORIGIN), is(ALLOW_ALL_ORIGINS));
    }

    @Test
    public void requestFromAnOriginHasResponseWithAllOriginsHeader() throws Exception {
        String origin = "http://www.ebi.ac.uk";

        MvcResult result = mockMvc.perform(
                get(RESOURCE_1_URL)
                        .headers(originHeader(origin)))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getHeader(ACCESS_CONTROL_ALLOW_ORIGIN), is(ALLOW_ALL_ORIGINS));
    }

    private HttpHeaders originHeader(String origin) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setOrigin(origin);
        return httpHeaders;
    }
}