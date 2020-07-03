package org.uniprot.api.unisave.controller;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.uniprot.api.unisave.UniSaveEntityMocker.*;

import java.util.List;

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
    private static final String ACCESSION = "P12345";
    private static final String INVALID_ACCESSION_FORMAT = "WRONG-FORMAT";
    private static final String RESOURCE_BASE = "/unisave/";
    private static final String ENTRY_VERSION = "$.results[*].entryVersion";
    private static final String RESULTS_CONTENT = "$.results[*].content";
    private static final String INVALID_ACCESSION_ERROR_MESSAGE =
            "The 'accession' value has invalid format. It should be a valid UniProtKB accession";
    private static final String STATUS = "/status";
    private static final String DIFF = "/diff";
    private static final String MESSAGES = "$.messages[*]";
    private static final String NOT_FOUND = "Resource not found";
    private static final String VERSIONS = "versions";
    @Autowired private UniSaveRepository uniSaveRepository;

    @Autowired private MockMvc mockMvc;

    // resource /{accession}
    @Test
    void canRetrieveEntriesWithoutContent() throws Exception {
        // given
        List<EntryInfoImpl> repositoryEntries =
                asList(mockEntryInfo(ACCESSION, 2), mockEntryInfo(ACCESSION, 1));
        doReturn(repositoryEntries).when(uniSaveRepository).retrieveEntryInfos(ACCESSION);

        // when
        ResultActions response =
                mockMvc.perform(
                        get(RESOURCE_BASE + ACCESSION).header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath(ENTRY_VERSION, contains(2, 1)))
                .andExpect(jsonPath(RESULTS_CONTENT).doesNotExist());
    }

    @Test
    void canRetrieveEntriesWithContent() throws Exception {
        // given
        List<EntryImpl> repositoryEntries =
                asList(mockEntry(ACCESSION, 2), mockEntry(ACCESSION, 1));
        doReturn(repositoryEntries).when(uniSaveRepository).retrieveEntries(ACCESSION);

        // when
        ResultActions response =
                mockMvc.perform(
                        get(RESOURCE_BASE + ACCESSION)
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param("includeContent", "true"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath(RESULTS_CONTENT, hasSize(2)))
                .andExpect(jsonPath("$.results[0].content").isNotEmpty())
                .andExpect(jsonPath("$.results[1].content").isNotEmpty());
    }

    @Test
    void canRetrieveSpecificVersionsOfEntriesWithoutContent() throws Exception {
        // given
        when(uniSaveRepository.retrieveEntryInfo(ACCESSION, 1))
                .thenReturn(mockEntryInfo(ACCESSION, 1));

        // when
        ResultActions response =
                mockMvc.perform(
                        get(RESOURCE_BASE + ACCESSION)
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param(VERSIONS, "1"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath(ENTRY_VERSION, contains(1)))
                .andExpect(jsonPath(RESULTS_CONTENT).doesNotExist());
    }

    @Test
    void canRetrieveSpecificVersionsOfEntriesWithContent() throws Exception {
        // given
        when(uniSaveRepository.retrieveEntry(ACCESSION, 1)).thenReturn(mockEntry(ACCESSION, 1));

        // when
        ResultActions response =
                mockMvc.perform(
                        get(RESOURCE_BASE + ACCESSION)
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param("includeContent", "true")
                                .param(VERSIONS, "1"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath(ENTRY_VERSION, contains(1)))
                .andExpect(jsonPath(RESULTS_CONTENT).exists());
    }

    @Test
    void canDownloadEntries() throws Exception {
        // given
        List<EntryInfoImpl> repositoryEntries =
                asList(mockEntryInfo(ACCESSION, 2), mockEntryInfo(ACCESSION, 1));
        doReturn(repositoryEntries).when(uniSaveRepository).retrieveEntryInfos(ACCESSION);

        // when
        ResultActions response =
                mockMvc.perform(
                        get(RESOURCE_BASE + ACCESSION)
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param("download", "true"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().exists(HttpHeaders.CONTENT_DISPOSITION));
    }

    @Test
    void nonExistentEntryCauses404() throws Exception {
        // given
        doThrow(ResourceNotFoundException.class)
                .when(uniSaveRepository)
                .retrieveEntryInfos(ACCESSION);

        // when
        ResultActions response =
                mockMvc.perform(
                        get(RESOURCE_BASE + ACCESSION).header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath(MESSAGES, contains(NOT_FOUND)));
    }

    @Test
    void invalidAccessionCausesBadRequest() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(RESOURCE_BASE + INVALID_ACCESSION_FORMAT)
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath(MESSAGES, contains(INVALID_ACCESSION_ERROR_MESSAGE)));
    }

    @Test
    void versionsParameterWithWrongFormatCausesBadRequest() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(RESOURCE_BASE + ACCESSION)
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param(VERSIONS, "XXXX"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        jsonPath(
                                MESSAGES,
                                contains(
                                        "Invalid request received. Comma separated version list must only contain non-zero integers, found: XXXX")));
    }

    // resource /{accession}/diff
    @Test
    void canGetDiffBetweenTwoVersions() throws Exception {
        // given
        DiffImpl diff = new DiffImpl();
        diff.setAccession(ACCESSION);
        diff.setDiff("mock diff");
        int version1 = 1;
        int version2 = 2;
        diff.setEntryOne(mockEntry(ACCESSION, version1));
        diff.setEntryTwo(mockEntry(ACCESSION, version2));

        when(uniSaveRepository.getDiff(ACCESSION, version1, version2)).thenReturn(diff);

        // when
        ResultActions response =
                mockMvc.perform(
                        get(RESOURCE_BASE + ACCESSION + DIFF)
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param("version1", String.valueOf(version1))
                                .param("version2", String.valueOf(version2)));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.diffInfo.entry1.entryVersion", is(1)))
                .andExpect(jsonPath("$.diffInfo.entry1.content").exists())
                .andExpect(jsonPath("$.diffInfo.entry2.entryVersion", is(2)))
                .andExpect(jsonPath("$.diffInfo.entry2.content").exists());
    }

    @Test
    void diffForInvalidAccessionCausesBadRequest() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(RESOURCE_BASE + INVALID_ACCESSION_FORMAT + DIFF)
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param("version1", "1")
                                .param("version2", "2"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath(MESSAGES, contains(INVALID_ACCESSION_ERROR_MESSAGE)));
    }

    @Test
    void diffForNonExistentEntryCauses404() throws Exception {
        // given
        doThrow(ResourceNotFoundException.class).when(uniSaveRepository).getDiff(ACCESSION, 1, 2);

        // when
        ResultActions response =
                mockMvc.perform(
                        get(RESOURCE_BASE + ACCESSION + DIFF)
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param("version1", "1")
                                .param("version2", "2"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath(MESSAGES, contains(NOT_FOUND)));
    }

    @Test
    void diffRequiresWithOnlyVersionOneCausesBadRequest() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(RESOURCE_BASE + ACCESSION + DIFF)
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param("version1", "1"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath(MESSAGES, contains("'version2' is a required parameter")));
    }

    @Test
    void diffRequiresWithOnlyVersionTwoCausesBadRequest() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(RESOURCE_BASE + ACCESSION + DIFF)
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param("version2", "1"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath(MESSAGES, contains("'version1' is a required parameter")));
    }

    // resource /{accession}/status
    @Test
    void canGetStatus() throws Exception {
        // given
        AccessionStatusInfoImpl status = new AccessionStatusInfoImpl();
        status.setAccession(ACCESSION);
        when(uniSaveRepository.retrieveEntryStatusInfo(ACCESSION)).thenReturn(status);

        // when
        ResultActions response =
                mockMvc.perform(
                        get(RESOURCE_BASE + ACCESSION + STATUS)
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.accession", is(ACCESSION)));
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
                        get(RESOURCE_BASE + accession + STATUS)
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.INTERNAL_SERVER_ERROR.value()))
                .andExpect(jsonPath(MESSAGES, contains("Internal server error")));
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
                        get(RESOURCE_BASE + accession + STATUS)
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath(MESSAGES, contains(NOT_FOUND)));
    }

    @Test
    void getStatusForInvalidAccessionCausesBadRequest() throws Exception { // given
        // when
        ResultActions response =
                mockMvc.perform(
                        get(RESOURCE_BASE + INVALID_ACCESSION_FORMAT + STATUS)
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath(MESSAGES, contains(INVALID_ACCESSION_ERROR_MESSAGE)));
    }

    @Profile("unisave-controller-test")
    @Configuration
    static class TestConfig {
        @Primary
        @Bean
        public UniSaveRepository uniSaveRepository() {
            UniSaveRepository mockRepository = mock(UniSaveRepository.class);
            when(mockRepository.getCurrentRelease()).thenReturn(mockRelease("1"));
            return mockRepository;
        }
    }
}
