package org.uniprot.api.rest.output.header;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.uniprot.api.rest.app.FakeController.FAKE_RESOURCE_1_URL;
import static org.uniprot.api.rest.app.FakeController.FAKE_RESOURCE_BASE;
import static org.uniprot.api.rest.output.header.HttpCommonHeaderConfig.ALLOW_ALL_ORIGINS;

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
import org.uniprot.api.rest.app.FakeRESTApp;
import org.uniprot.api.rest.download.AsyncDownloadMocks;

/**
 * Check that a REST app that picks up an {@link HttpCommonHeaderConfig} will show explicitly the
 * all origins header value, '*' -- and not the actual origin.
 *
 * <p>Created 13/03/18
 *
 * @author Edd
 */
@ActiveProfiles("use-fake-app")
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {AsyncDownloadMocks.class, FakeRESTApp.class})
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
                mockMvc.perform(get(FAKE_RESOURCE_BASE + FAKE_RESOURCE_1_URL))
                        .andDo(log())
                        .andExpect(status().isOk())
                        .andReturn();

        assertThat(
                result.getResponse().getHeader(ACCESS_CONTROL_ALLOW_ORIGIN), is(ALLOW_ALL_ORIGINS));
    }

    @Test
    void requestFromAnOriginHasResponseWithAllOriginsHeader() throws Exception {
        String origin = "http://www.ebi.ac.uk";

        MvcResult result =
                mockMvc.perform(
                                get(FAKE_RESOURCE_BASE + FAKE_RESOURCE_1_URL)
                                        .headers(originHeader(origin)))
                        .andDo(log())
                        .andExpect(status().isOk())
                        .andReturn();

        assertThat(
                result.getResponse().getHeader(ACCESS_CONTROL_ALLOW_ORIGIN), is(ALLOW_ALL_ORIGINS));
    }

    @Test
    void requestHasXReleaseNumberHeader() throws Exception {
        MvcResult result =
                mockMvc.perform(get(FAKE_RESOURCE_BASE + FAKE_RESOURCE_1_URL))
                        .andDo(log())
                        .andExpect(status().isOk())
                        .andReturn();

        assertThat(
                result.getResponse().getHeader(HttpCommonHeaderConfig.X_UNIPROT_RELEASE),
                is("2021_02 stub release file"));
    }

    @Test
    void requestHasXReleaseDateHeader() throws Exception {
        MvcResult result =
                mockMvc.perform(get(FAKE_RESOURCE_BASE + FAKE_RESOURCE_1_URL))
                        .andDo(log())
                        .andExpect(status().isOk())
                        .andReturn();

        assertThat(
                result.getResponse().getHeader(HttpCommonHeaderConfig.X_UNIPROT_RELEASE_DATE),
                is("20-Apr-2021"));
    }

    @Test
    void requestHasXAPIDeploymentDateHeader() throws Exception {
        MvcResult result =
                mockMvc.perform(get(FAKE_RESOURCE_BASE + FAKE_RESOURCE_1_URL))
                        .andDo(log())
                        .andExpect(status().isOk())
                        .andReturn();

        assertThat(
                result.getResponse().getHeader(HttpCommonHeaderConfig.X_API_DEPLOYMENT_DATE),
                is("06-Jun-2022"));
    }

    private HttpHeaders originHeader(String origin) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setOrigin(origin);
        return httpHeaders;
    }
}
