package org.uniprot.api.help.centre.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.uniprot.api.help.centre.HelpCentreRestApplication;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;

@ContextConfiguration(classes = {HelpCentreRestApplication.class, ErrorHandlerConfig.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(ContactController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
        })
class ContactControllerIT {

    private static final String GENERATE_TOKEN_PATH = "/contact/token";
    private static final String SEND_CONTECT_PATH = "/contact/send";

    @Autowired private MockMvc mockMvc;

    @Test
    void generateTokenReturnSuccess() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(GENERATE_TOKEN_PATH)
                        .param("key", "keyValue")
                        .header(ACCEPT, MediaType.APPLICATION_JSON);

        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.token", not(emptyOrNullString())));
    }

    @Test
    void generateTokenMissingRequiredParameterReturnError() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(GENERATE_TOKEN_PATH).header(ACCEPT, MediaType.APPLICATION_JSON);

        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.url", not(emptyOrNullString())))
                .andExpect(jsonPath("$.messages.*", contains("key is a required parameter")));
    }

    @Test
    void postContactReturnSuccess() throws Exception {
        // given
        String email = "email@valid.com";
        String subject = "subjectValue";
        String message = "messageValue";
        MockHttpServletRequestBuilder requestBuilder =
                get(GENERATE_TOKEN_PATH)
                        .param("key", subject)
                        .header(ACCEPT, MediaType.APPLICATION_JSON);

        ResultActions response = mockMvc.perform(requestBuilder);
        String tokenResponse = response.andReturn().getResponse().getContentAsString();
        String token = tokenResponse.split(":")[1].split("\"")[1];

        // when
        requestBuilder =
                post(SEND_CONTECT_PATH)
                        .param("email", email)
                        .param("subject", subject)
                        .param("message", message)
                        .param("token", token)
                        .param("requiredForRobots", "")
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .header(ACCEPT, MediaType.APPLICATION_JSON);

        response = mockMvc.perform(requestBuilder);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.email", is(email)))
                .andExpect(jsonPath("$.subject", is(subject)))
                .andExpect(jsonPath("$.message", is(message)))
                .andExpect(jsonPath("$.token", is(token)))
                .andExpect(jsonPath("$.requiredForRobots", is(emptyString())));
    }

    @Test
    void postContactMissingRequiredParameterReturnError() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                post(SEND_CONTECT_PATH)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .header(ACCEPT, MediaType.APPLICATION_JSON);

        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.url", not(emptyOrNullString())))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                containsInAnyOrder(
                                        "The 'email' is a required field",
                                        "The 'subject' is a required field",
                                        "The 'message' is a required field",
                                        "Missing data. Please use the contact page on www.uniprot.org",
                                        "Please use the contact page on www.uniprot.org")));
        // IMPORTANT: token and requiredForRobots validation has generic error messages because
        // they are security validations. We do not want to explain how to bypass our security
        // checks
    }

    @Test
    void postContactHoneyPotParameterReturnError() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                post(SEND_CONTECT_PATH)
                        .param("email", "email@test.com")
                        .param("subject", "Subject")
                        .param("message", "Message")
                        .param("token", "Token")
                        .param("requiredForRobots", "BotValue")
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .header(ACCEPT, MediaType.APPLICATION_JSON);

        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.url", not(emptyOrNullString())))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains("Please use the contact page on www.uniprot.org")));
    }

    @Test
    void postContactInvalidEmailParameterReturnError() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                post(SEND_CONTECT_PATH)
                        .param("email", "Invalid")
                        .param("subject", "Subject")
                        .param("message", "Message")
                        .param("token", "Token")
                        .param("requiredForRobots", "")
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .header(ACCEPT, MediaType.APPLICATION_JSON);

        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.url", not(emptyOrNullString())))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains(
                                        "The 'email' is invalid. It must have a valid e-mail address")));
    }
}
