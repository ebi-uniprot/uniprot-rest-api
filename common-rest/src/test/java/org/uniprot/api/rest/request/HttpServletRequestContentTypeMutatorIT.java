package org.uniprot.api.rest.request;

import static org.hamcrest.core.StringContains.containsString;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.uniprot.api.rest.app.FakeController.EXPECTED_NUMBER;
import static org.uniprot.api.rest.app.FakeController.FAKE_RESOURCE_BASE;
import static org.uniprot.api.rest.output.UniProtMediaType.*;
import static org.uniprot.api.rest.request.HttpServletRequestContentTypeMutator.FORMAT;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.uniprot.api.rest.app.FakeController;
import org.uniprot.api.rest.app.FakeRESTApp;

/**
 * The purpose of this test is to guarantee the {@link HttpServletRequestContentTypeMutator} class
 * is mutating correctly requests when incorrect content-types/extensions/formats are specified.
 *
 * <p>Created 29/04/2020
 *
 * @author Edd
 */
@ActiveProfiles("use-fake-app")
@ContextConfiguration(classes = {FakeRESTApp.class})
@ExtendWith(SpringExtension.class)
@WebMvcTest(FakeController.class)
@AutoConfigureWebClient
public class HttpServletRequestContentTypeMutatorIT {
    @Autowired private MockMvc mockMvc;

    @Test
    void canGetResourceWithAcceptableAcceptHeader() throws Exception {
        String mediaType = TSV_MEDIA_TYPE_VALUE;
        ResultActions response =
                mockMvc.perform(get(FAKE_RESOURCE_BASE + "/resource").header(ACCEPT, mediaType));

        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString(mediaType)));
    }

    @Test
    void canGetResourceWithAcceptableExtension() throws Exception {
        ResultActions response = mockMvc.perform(get(FAKE_RESOURCE_BASE + "/resource/ID.tsv"));

        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_TYPE,
                                        containsString(TSV_MEDIA_TYPE_VALUE)));
    }

    @Test
    void canGetResourceWithAcceptableFormat() throws Exception {
        ResultActions response =
                mockMvc.perform(get(FAKE_RESOURCE_BASE + "/resource").param("format", "tsv"));

        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_TYPE,
                                        containsString(TSV_MEDIA_TYPE_VALUE)));
    }

    @Test
    void badRequestWithValidButNotAcceptedAcceptHeader() throws Exception {
        ResultActions response =
                mockMvc.perform(
                        get(FAKE_RESOURCE_BASE + "/resource").header(ACCEPT, OBO_MEDIA_TYPE_VALUE));

        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "Invalid request received. Requested media type/format not accepted: 'text/obo'.")))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_TYPE,
                                        containsString(DEFAULT_MEDIA_TYPE_VALUE)));
    }

    @Test
    void badRequestWithValidButNotAcceptedExtension() throws Exception {
        ResultActions response = mockMvc.perform(get(FAKE_RESOURCE_BASE + "/resource/ID.obo"));

        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "Invalid request received. Requested media type/format not accepted, 'obo'. Valid media types/formats for this end-point include: text/tsv, text/flatfile.")))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_TYPE,
                                        containsString(DEFAULT_MEDIA_TYPE_VALUE)));
    }

    @Test
    void badRequestWithValidButNotAcceptedFormat() throws Exception {
        ResultActions response =
                mockMvc.perform(get(FAKE_RESOURCE_BASE + "/resource/ID").param(FORMAT, "obo"));

        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "Invalid request received. Requested media type/format not accepted, 'obo'. Valid media types/formats for this end-point include: text/tsv, text/flatfile.")))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_TYPE,
                                        containsString(DEFAULT_MEDIA_TYPE_VALUE)));
    }

    @Test
    void nonExistentResourcesCauses404() throws Exception {
        ResultActions response =
                mockMvc.perform(
                        get(FAKE_RESOURCE_BASE + "/THIS_DOES_NOT_EXIST")
                                .header(ACCEPT, DEFAULT_MEDIA_TYPE));

        response.andDo(print()).andExpect(status().is(HttpStatus.NOT_FOUND.value()));
    }

    @Test
    void validNumberIdWithValidFormatRequestSucceeds() throws Exception {
        ResultActions response =
                mockMvc.perform(
                        get(FAKE_RESOURCE_BASE + "/resource/number/12").param("format", "txt"));

        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_TYPE,
                                        containsString(FF_MEDIA_TYPE_VALUE)));
    }

    @Test
    void validNumberIdWithValidExtensionRequestSucceeds() throws Exception {
        ResultActions response =
                mockMvc.perform(get(FAKE_RESOURCE_BASE + "/resource/number/12.txt"));

        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_TYPE,
                                        containsString(FF_MEDIA_TYPE_VALUE)));
    }

    @Test
    void invalidNumberIdRequestCausesError() throws Exception {
        ResultActions response =
                mockMvc.perform(
                        get(FAKE_RESOURCE_BASE + "/resource/number/wrong")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(content().string(containsString(EXPECTED_NUMBER)))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_TYPE,
                                        containsString(APPLICATION_JSON_VALUE)));
    }

    @Test
    void validNumberButInvalidExtensionCausesError() throws Exception {
        ResultActions response =
                mockMvc.perform(get(FAKE_RESOURCE_BASE + "/resource/number/12.xxxx"));

        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(content().string(containsString(EXPECTED_NUMBER)))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_TYPE,
                                        containsString(APPLICATION_JSON_VALUE)));
    }

    @Test
    void validNumberButInvalidFormatCausesError() throws Exception {
        ResultActions response =
                mockMvc.perform(
                        get(FAKE_RESOURCE_BASE + "/resource/number/12").param(FORMAT, "xxxx"));

        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "Invalid request received. Requested media type/format not accepted: 'xxxx'.")))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_TYPE,
                                        containsString(APPLICATION_JSON_VALUE)));
    }

    @Test
    void validNumberValidButNotAcceptedExtensionCausesError() throws Exception {
        ResultActions response =
                mockMvc.perform(get(FAKE_RESOURCE_BASE + "/resource/number/12.obo"));

        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "Invalid request received. Requested media type/format not accepted, 'obo'. Valid media types/formats for this end-point include: application/json, text/flatfile.")))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_TYPE,
                                        containsString(APPLICATION_JSON_VALUE)));
    }
}
