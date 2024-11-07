package org.uniprot.api.unisave.controller;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
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
import org.junit.jupiter.api.TestInstance;
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
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.unisave.UniSaveRESTApplication;
import org.uniprot.api.unisave.repository.UniSaveRepository;
import org.uniprot.api.unisave.repository.domain.EntryInfo;
import org.uniprot.api.unisave.repository.domain.EventTypeEnum;
import org.uniprot.api.unisave.repository.domain.impl.*;
import org.uniprot.core.uniprotkb.DeletedReason;

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
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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
    void canRetrieveAllAggregatedEntries() throws Exception {
        // given
        List<EntryImpl> repositoryEntries =
                asList(
                        mockEntry(ACCESSION, 4, 2, true),
                        mockEntry(ACCESSION, 3, 2, true),
                        mockEntry(ACCESSION, 2, 1, true),
                        mockEntry(ACCESSION, 1, 1, true));
        doReturn(repositoryEntries).when(uniSaveRepository).retrieveEntries(ACCESSION);

        // when
        ResultActions response =
                mockMvc.perform(
                        get(RESOURCE_BASE + ACCESSION)
                                .header(ACCEPT, UniProtMediaType.FASTA_MEDIA_TYPE_VALUE)
                                .param("uniqueSequences", "true"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_TYPE,
                                        UniProtMediaType.FASTA_MEDIA_TYPE_VALUE))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                """
                                                        >P12345: EV=3-4 SV=2
                                                        MASGAYSKYLFQIIGETVSSTNRGNKYNSFDHSRVDTRAGSFREAYNSKKKGSGRFGRKC
                                                        FQIIGETVSSTNRG
                                                        >P12345: EV=1-2 SV=1
                                                        MASGAYSKYLFQIIGETVSSTNRGNKYNSFDHSRVDTRAGSFREAYNSKKKGSGRFGRKC
                                                        FQIIGETVSSTNRG""")));
    }

    @Test
    void canRetrieveAggregatedEntriesInVersionRange() throws Exception {
        // given
        when(uniSaveRepository.retrieveEntry(ACCESSION, 4))
                .thenReturn(mockEntry(ACCESSION, 4, 2, true));
        when(uniSaveRepository.retrieveEntry(ACCESSION, 3))
                .thenReturn(mockEntry(ACCESSION, 3, 2, true));
        when(uniSaveRepository.retrieveEntry(ACCESSION, 2))
                .thenReturn(mockEntry(ACCESSION, 2, 1, true));
        when(uniSaveRepository.retrieveEntry(ACCESSION, 1))
                .thenReturn(mockEntry(ACCESSION, 1, 1, true));

        // when
        ResultActions response =
                mockMvc.perform(
                        get(RESOURCE_BASE + ACCESSION)
                                .header(ACCEPT, UniProtMediaType.FASTA_MEDIA_TYPE_VALUE)
                                .param("uniqueSequences", "true")
                                .param("versions", "2-4"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_TYPE,
                                        UniProtMediaType.FASTA_MEDIA_TYPE_VALUE))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                """
                                                        >P12345: EV=3-4 SV=2
                                                        MASGAYSKYLFQIIGETVSSTNRGNKYNSFDHSRVDTRAGSFREAYNSKKKGSGRFGRKC
                                                        FQIIGETVSSTNRG
                                                        >P12345: EV=2 SV=1
                                                        MASGAYSKYLFQIIGETVSSTNRGNKYNSFDHSRVDTRAGSFREAYNSKKKGSGRFGRKC
                                                        FQIIGETVSSTNRG""")));
    }

    @Test
    void canRetrieveAggregatedEntriesForCommaSeparatedValues() throws Exception {
        // given
        when(uniSaveRepository.retrieveEntry(ACCESSION, 4))
                .thenReturn(mockEntry(ACCESSION, 4, 2, true));
        when(uniSaveRepository.retrieveEntry(ACCESSION, 3))
                .thenReturn(mockEntry(ACCESSION, 3, 2, true));
        when(uniSaveRepository.retrieveEntry(ACCESSION, 2))
                .thenReturn(mockEntry(ACCESSION, 2, 1, true));
        when(uniSaveRepository.retrieveEntry(ACCESSION, 1))
                .thenReturn(mockEntry(ACCESSION, 1, 1, true));

        // when
        ResultActions response =
                mockMvc.perform(
                        get(RESOURCE_BASE + ACCESSION)
                                .header(ACCEPT, UniProtMediaType.FASTA_MEDIA_TYPE_VALUE)
                                .param("uniqueSequences", "true")
                                .param("versions", "2,3"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_TYPE,
                                        UniProtMediaType.FASTA_MEDIA_TYPE_VALUE))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                """
                                                        >P12345: EV=3 SV=2
                                                        MASGAYSKYLFQIIGETVSSTNRGNKYNSFDHSRVDTRAGSFREAYNSKKKGSGRFGRKC
                                                        FQIIGETVSSTNRG
                                                        >P12345: EV=2 SV=1
                                                        MASGAYSKYLFQIIGETVSSTNRGNKYNSFDHSRVDTRAGSFREAYNSKKKGSGRFGRKC
                                                        FQIIGETVSSTNRG""")));
    }

    @Test
    void canRetrieveAggregatedEntriesCommaSeparatedValuesRangeMix() throws Exception {
        // given
        when(uniSaveRepository.retrieveEntry(ACCESSION, 7))
                .thenReturn(mockEntry(ACCESSION, 7, 4, true));
        when(uniSaveRepository.retrieveEntry(ACCESSION, 6))
                .thenReturn(mockEntry(ACCESSION, 6, 3, true));
        when(uniSaveRepository.retrieveEntry(ACCESSION, 5))
                .thenReturn(mockEntry(ACCESSION, 5, 2, true));
        when(uniSaveRepository.retrieveEntry(ACCESSION, 4))
                .thenReturn(mockEntry(ACCESSION, 4, 2, true));
        when(uniSaveRepository.retrieveEntry(ACCESSION, 3))
                .thenReturn(mockEntry(ACCESSION, 3, 2, true));
        when(uniSaveRepository.retrieveEntry(ACCESSION, 2))
                .thenReturn(mockEntry(ACCESSION, 2, 1, true));
        when(uniSaveRepository.retrieveEntry(ACCESSION, 1))
                .thenReturn(mockEntry(ACCESSION, 1, 1, true));

        // when
        ResultActions response =
                mockMvc.perform(
                        get(RESOURCE_BASE + ACCESSION)
                                .header(ACCEPT, UniProtMediaType.FASTA_MEDIA_TYPE_VALUE)
                                .param("uniqueSequences", "true")
                                .param("versions", "1,3-5,7"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_TYPE,
                                        UniProtMediaType.FASTA_MEDIA_TYPE_VALUE))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                """
                                                        >P12345: EV=7 SV=4
                                                        MASGAYSKYLFQIIGETVSSTNRGNKYNSFDHSRVDTRAGSFREAYNSKKKGSGRFGRKC
                                                        FQIIGETVSSTNRG
                                                        >P12345: EV=3-5 SV=2
                                                        MASGAYSKYLFQIIGETVSSTNRGNKYNSFDHSRVDTRAGSFREAYNSKKKGSGRFGRKC
                                                        FQIIGETVSSTNRG
                                                        >P12345: EV=1 SV=1
                                                        MASGAYSKYLFQIIGETVSSTNRGNKYNSFDHSRVDTRAGSFREAYNSKKKGSGRFGRKC
                                                        FQIIGETVSSTNRG""")));
    }

    @Test
    void retrievingAggregatedEntriesWithContentFalseStillWorks() throws Exception {
        // given
        List<EntryImpl> repositoryEntries =
                asList(
                        mockEntry(ACCESSION, 4, 2, true),
                        mockEntry(ACCESSION, 3, 2, true),
                        mockEntry(ACCESSION, 2, 1, true),
                        mockEntry(ACCESSION, 1, 1, true));
        doReturn(repositoryEntries).when(uniSaveRepository).retrieveEntries(ACCESSION);

        // when
        ResultActions response =
                mockMvc.perform(
                        get(RESOURCE_BASE + ACCESSION)
                                .header(ACCEPT, UniProtMediaType.FASTA_MEDIA_TYPE_VALUE)
                                .param("uniqueSequences", "true")
                                .param("includeContent", "true"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_TYPE,
                                        UniProtMediaType.FASTA_MEDIA_TYPE_VALUE))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                """
                                                        >P12345: EV=3-4 SV=2
                                                        MASGAYSKYLFQIIGETVSSTNRGNKYNSFDHSRVDTRAGSFREAYNSKKKGSGRFGRKC
                                                        FQIIGETVSSTNRG
                                                        >P12345: EV=1-2 SV=1
                                                        MASGAYSKYLFQIIGETVSSTNRGNKYNSFDHSRVDTRAGSFREAYNSKKKGSGRFGRKC
                                                        FQIIGETVSSTNRG""")));
    }

    @Test
    void retrievingAllAggregatedEntriesInTSV() throws Exception {
        // given
        List<EntryInfo> repositoryEntries =
                asList(
                        mockEntryInfo(ACCESSION, 4),
                        mockEntryInfo(ACCESSION, 3),
                        mockEntryInfo(ACCESSION, 2),
                        mockEntryInfo(ACCESSION, 1));
        doReturn(repositoryEntries).when(uniSaveRepository).retrieveEntryInfos(ACCESSION);

        // when
        ResultActions response =
                mockMvc.perform(
                        get(RESOURCE_BASE + ACCESSION)
                                .header(ACCEPT, UniProtMediaType.TSV_MEDIA_TYPE_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_TYPE,
                                        UniProtMediaType.TSV_MEDIA_TYPE_VALUE))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                """
                                                        Entry version\tSequence version\tEntry name\tDatabase\tNumber\tDate\tReplaces\tReplaced by
                                                        4\t0\tname\tSwiss-Prot\t2\t15-Nov-2021\t\t
                                                        3\t0\tname\tSwiss-Prot\t2\t15-Nov-2021\t\t
                                                        2\t0\tname\tSwiss-Prot\t2\t15-Nov-2021\t\t
                                                        1\t0\tname\tSwiss-Prot\t2\t15-Nov-2021\t\t
                                                        """)));
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
                .andExpect(jsonPath(MESSAGES).value(contains(NOT_FOUND)));
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
                .andExpect(jsonPath(MESSAGES).value(contains(INVALID_ACCESSION_ERROR_MESSAGE)));
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
                        jsonPath(MESSAGES)
                                .value(
                                        contains(
                                                "Invalid request received. Version list must contain integers greater than zero. For example, 1-5,8,20-30. Instead, found: XXXX")));
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
                .andExpect(jsonPath(MESSAGES).value(contains(INVALID_ACCESSION_ERROR_MESSAGE)));
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
                .andExpect(jsonPath(MESSAGES).value(contains(NOT_FOUND)));
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
                .andExpect(
                        jsonPath(MESSAGES).value(contains("'version2' is a required parameter")));
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
                .andExpect(
                        jsonPath(MESSAGES).value(contains("'version1' is a required parameter")));
    }

    // resource /{accession}/status
    @Test
    void canGetStatusForDeletedEntries() throws Exception {
        // given
        AccessionStatusInfoImpl status = new AccessionStatusInfoImpl();
        status.setAccession(ACCESSION);
        IdentifierStatus event = mockIdentifierStatus(EventTypeEnum.DELETED, ACCESSION, "", 1);
        event.setEventRelease(mockRelease("1"));
        status.setEvents(List.of(event));
        when(uniSaveRepository.retrieveEntryStatusInfo(ACCESSION)).thenReturn(status);

        // when
        ResultActions response =
                mockMvc.perform(
                        get(RESOURCE_BASE + ACCESSION + STATUS)
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.accession", is(ACCESSION)))
                .andExpect(jsonPath("$.events.size()", is(1)))
                .andExpect(jsonPath("$.events[0].eventType", is(EventTypeEnum.DELETED.toString())))
                .andExpect(jsonPath("$.events[0].release", is("1")))
                .andExpect(
                        jsonPath(
                                "$.events[0].deletedReason",
                                is(DeletedReason.SOURCE_DELETION_EMBL.getName())));
    }

    @Test
    void canGetStatusForUnknownDeletedEntries() throws Exception {
        // given
        AccessionStatusInfoImpl status = new AccessionStatusInfoImpl();
        status.setAccession(ACCESSION);
        IdentifierStatus event = mockIdentifierStatus(EventTypeEnum.DELETED, ACCESSION, "", 12);
        event.setEventRelease(mockRelease("1"));
        status.setEvents(List.of(event));
        when(uniSaveRepository.retrieveEntryStatusInfo(ACCESSION)).thenReturn(status);

        // when
        ResultActions response =
                mockMvc.perform(
                        get(RESOURCE_BASE + ACCESSION + STATUS)
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.accession", is(ACCESSION)))
                .andExpect(jsonPath("$.events.size()", is(1)))
                .andExpect(jsonPath("$.events[0].eventType", is(EventTypeEnum.DELETED.toString())))
                .andExpect(jsonPath("$.events[0].release", is("1")))
                .andExpect(jsonPath("$.events[0].deletedReason").doesNotExist());
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
                .andExpect(jsonPath(MESSAGES).value(contains("Internal server error")));
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
                .andExpect(jsonPath(MESSAGES).value(contains(NOT_FOUND)));
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
                .andExpect(jsonPath(MESSAGES).value(contains(INVALID_ACCESSION_ERROR_MESSAGE)));
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
