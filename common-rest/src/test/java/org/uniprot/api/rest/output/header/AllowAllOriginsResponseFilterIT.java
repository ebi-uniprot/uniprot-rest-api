package org.uniprot.api.rest.output.header;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.OncePerRequestFilter;
import org.uniprot.api.rest.output.app.FakeRESTApp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.uniprot.api.rest.output.app.FakeRESTApp.RESOURCE_1_URL;
import static org.uniprot.api.rest.output.header.HttpCommonHeaderConfig.ALLOW_ALL_ORIGINS;

/**
 * Check that a REST app that picks up an {@link HttpCommonHeaderConfig} will show explicitly the
 * all origins header value, '*' -- and not the actual origin.
 *
 * <p>Created 13/03/18
 *
 * @author Edd
 */
@ActiveProfiles("allow-origins-integration-test")
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {FakeRESTApp.class})
@WebAppConfiguration
class AllowAllOriginsResponseFilterIT {
    @Autowired private WebApplicationContext webApplicationContext;
    private MockMvc mockMvc;

    @Qualifier("oncePerRequestFilter")
    @Autowired
    private OncePerRequestFilter originsFilter;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.webAppContextSetup(webApplicationContext)
                        .addFilter(originsFilter)
                        .build();
    }

    @Test
    void requestWithoutAnOriginHasResponseWithAllOriginsHeader() throws Exception {
        MvcResult result =
                mockMvc.perform(get(RESOURCE_1_URL))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andReturn();

        assertThat(
                result.getResponse().getHeader(ACCESS_CONTROL_ALLOW_ORIGIN), is(ALLOW_ALL_ORIGINS));
    }

    @Test
    void requestFromAnOriginHasResponseWithAllOriginsHeader() throws Exception {
        String origin = "http://www.ebi.ac.uk";

        MvcResult result =
                mockMvc.perform(get(RESOURCE_1_URL).headers(originHeader(origin)))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andReturn();

        assertThat(
                result.getResponse().getHeader(ACCESS_CONTROL_ALLOW_ORIGIN), is(ALLOW_ALL_ORIGINS));
    }

    @Test
    void requestHasXReleaseHeader() throws Exception {
        MvcResult result =
                mockMvc.perform(get(RESOURCE_1_URL))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andReturn();

        assertThat(
                result.getResponse().getHeader(HttpCommonHeaderConfig.X_RELEASE),
                is("2020_02 stub release file"));
    }

    private HttpHeaders originHeader(String origin) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setOrigin(origin);
        return httpHeaders;
    }
}
