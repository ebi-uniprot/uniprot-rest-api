package org.uniprot.api.unisave.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.repository.search.QueryRetrievalException;
import org.uniprot.api.unisave.UniSaveRESTApplication;
import org.uniprot.api.unisave.repository.UniSaveRepository;
import org.uniprot.api.unisave.repository.domain.impl.AccessionStatusInfoImpl;
import org.uniprot.api.unisave.repository.domain.impl.DiffImpl;
import org.uniprot.api.unisave.repository.domain.impl.EntryImpl;
import org.uniprot.api.unisave.repository.domain.impl.EntryInfoImpl;

import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.uniprot.api.unisave.UniSaveEntityMocker.mockEntry;
import static org.uniprot.api.unisave.UniSaveEntityMocker.mockEntryInfo;

/**
 * Created 06/04/20
 *
 * @author Edd
 */
@ActiveProfiles("unisave-controller-test")
@ExtendWith(SpringExtension.class)
@AutoConfigureWebClient
@ContextConfiguration(
        classes = {UniSaveControllerTest.TestConfig.class, UniSaveRESTApplication.class})
@WebMvcTest(UniSaveController.class)
class UniSaveControllerTest {

    @Autowired private UniSaveRepository uniSaveRepository;

    @Autowired private MockMvc mockMvc;

    // resource /{accession}
    @Test
    void canRetrieveEntriesWithoutContent() throws Exception {
        // given
        String accession = "P12345";
        List<EntryInfoImpl> repositoryEntries =
                asList(mockEntryInfo(accession, 2), mockEntryInfo(accession, 1));
        doReturn(repositoryEntries).when(uniSaveRepository).retrieveEntryInfos(accession);

        // when
        ResultActions response =
                mockMvc.perform(
                        get("/unisave/" + accession).header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results[*].entryVersion", contains(2, 1)))
                .andExpect(jsonPath("$.results[*].content").doesNotExist());
    }

    @Test
    void canRetrieveEntriesWithContent() throws Exception {
        // given
        String accession = "P12345";
        List<EntryImpl> repositoryEntries =
                asList(mockEntry(accession, 2), mockEntry(accession, 1));
        doReturn(repositoryEntries).when(uniSaveRepository).retrieveEntries(accession);

        // when
        ResultActions response =
                mockMvc.perform(
                        get("/unisave/" + accession)
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param("includeContent", "true"));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results[*].content", hasSize(2)))
                .andExpect(jsonPath("$.results[0].content").isNotEmpty())
                .andExpect(jsonPath("$.results[1].content").isNotEmpty());
    }

    @Test
    void canRetrieveSpecificVersionsOfEntriesWithoutContent() throws Exception {
        // given
        String accession = "P12345";
        when(uniSaveRepository.retrieveEntryInfo(accession, 1))
                .thenReturn(mockEntryInfo(accession, 1));

        // when
        ResultActions response =
                mockMvc.perform(
                        get("/unisave/" + accession)
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param("versions", "1"));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results[*].entryVersion", contains(1)))
                .andExpect(jsonPath("$.results[*].content").doesNotExist());
    }

    @Test
    void canRetrieveSpecificVersionsOfEntriesWithContent() throws Exception {
        // given
        String accession = "P12345";
        when(uniSaveRepository.retrieveEntry(accession, 1)).thenReturn(mockEntry(accession, 1));

        // when
        ResultActions response =
                mockMvc.perform(
                        get("/unisave/" + accession)
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param("includeContent", "true")
                                .param("versions", "1"));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results[*].entryVersion", contains(1)))
                .andExpect(jsonPath("$.results[*].content").exists());
    }

    @Test
    void canDownloadEntries() throws Exception {
        // given
        String accession = "P12345";
        List<EntryInfoImpl> repositoryEntries =
                asList(mockEntryInfo(accession, 2), mockEntryInfo(accession, 1));
        doReturn(repositoryEntries).when(uniSaveRepository).retrieveEntryInfos(accession);

        // when
        ResultActions response =
                mockMvc.perform(
                        get("/unisave/" + accession)
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param("download", "true"));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().exists(HttpHeaders.CONTENT_DISPOSITION));
    }

    @Test
    void nonExistentEntryCauses404() throws Exception {
        // given
        String accession = "P12345";
        doThrow(ResourceNotFoundException.class)
                .when(uniSaveRepository)
                .retrieveEntryInfos(accession);

        // when
        ResultActions response =
                mockMvc.perform(
                        get("/unisave/" + accession).header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.messages[*]", contains("Resource not found")));
    }

    @Test
    void versionsParameterWithWrongFormatCausesBadRequest() throws Exception {
        // given
        String accession = "P12345";

        // when
        ResultActions response =
                mockMvc.perform(
                        get("/unisave/" + accession)
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param("versions", "XXXX"));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        jsonPath(
                                "$.messages[*]",
                                contains(
                                        "Invalid request received. Comma separated version list must only contain non-zero integers, found: XXXX")));
    }

    // resource /{accession}/diff
    @Test
    void canGetDiffBetweenTwoVersions() throws Exception {
        // given
        String accession = "P12345";
        DiffImpl diff = new DiffImpl();
        diff.setAccession(accession);
        diff.setDiff("mock diff");
        int version1 = 1;
        int version2 = 2;
        diff.setEntryOne(mockEntry(accession, version1));
        diff.setEntryTwo(mockEntry(accession, version2));

        when(uniSaveRepository.getDiff(accession, version1, version2)).thenReturn(diff);

        // when
        ResultActions response =
                mockMvc.perform(
                        get("/unisave/" + accession + "/diff")
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param("version1", String.valueOf(version1))
                                .param("version2", String.valueOf(version2)));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.diffInfo.entry1.entryVersion", is(1)))
                .andExpect(jsonPath("$.diffInfo.entry1.content").exists())
                .andExpect(jsonPath("$.diffInfo.entry2.entryVersion", is(2)))
                .andExpect(jsonPath("$.diffInfo.entry2.content").exists());
    }

    @Test
    void diffForNonExistentEntryCauses404() throws Exception {
        // given
        String accession = "P12345";
        doThrow(ResourceNotFoundException.class).when(uniSaveRepository).getDiff(accession, 1, 2);

        // when
        ResultActions response =
                mockMvc.perform(
                        get("/unisave/" + accession + "/diff")
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param("version1", "1")
                                .param("version2", "2"));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.messages[*]", contains("Resource not found")));
    }

    @Test
    void diffRequiresWithOnlyVersionOneCausesBadRequest() throws Exception {
        // given
        String accession = "P12345";

        // when
        ResultActions response =
                mockMvc.perform(
                        get("/unisave/" + accession + "/diff")
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param("version1", "1"));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        jsonPath("$.messages[*]", contains("'version2' is a required parameter")));
    }

    @Test
    void diffRequiresWithOnlyVersionTwoCausesBadRequest() throws Exception {
        // given
        String accession = "P12345";

        // when
        ResultActions response =
                mockMvc.perform(
                        get("/unisave/" + accession + "/diff")
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param("version2", "1"));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        jsonPath("$.messages[*]", contains("'version1' is a required parameter")));
    }

    // resource /{accession}/status
    @Test
    void canGetStatus() throws Exception { // given
        String accession = "P12345";
        AccessionStatusInfoImpl status = new AccessionStatusInfoImpl();
        status.setAccession(accession);
        when(uniSaveRepository.retrieveEntryStatusInfo(accession)).thenReturn(status);

        // when
        ResultActions response =
                mockMvc.perform(
                        get("/unisave/" + accession + "/status")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.accession", is(accession)));
    }

    @Test
    void getStatusWhenRepositoryExceptionCauses500() throws Exception { // given
        String accession = "P12346";
        doThrow(QueryRetrievalException.class)
                .when(uniSaveRepository)
                .retrieveEntryStatusInfo(accession);

        // when
        ResultActions response =
                mockMvc.perform(
                        get("/unisave/" + accession + "/status")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.INTERNAL_SERVER_ERROR.value()))
                .andExpect(jsonPath("$.messages[*]", contains("Internal server error")));
    }

    @Test
    void statusForNonExistentEntryCauses404() throws Exception {
        // given
        String accession = "P12347";
        doThrow(ResourceNotFoundException.class)
                .when(uniSaveRepository)
                .retrieveEntryStatusInfo(accession);

        // when
        ResultActions response =
                mockMvc.perform(
                        get("/unisave/" + accession + "/status")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.messages[*]", contains("Resource not found")));
    }

    @Profile("unisave-controller-test")
    @Configuration
    static class TestConfig {
        @Primary
        @Bean
        public UniSaveRepository uniSaveRepository() {
            return mock(UniSaveRepository.class);
        }
    }
}
