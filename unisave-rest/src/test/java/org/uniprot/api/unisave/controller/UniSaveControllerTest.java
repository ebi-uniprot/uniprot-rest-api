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
import org.uniprot.api.unisave.UniSaveRESTApplication;
import org.uniprot.api.unisave.repository.UniSaveRepository;
import org.uniprot.api.unisave.repository.domain.DatabaseEnum;
import org.uniprot.api.unisave.repository.domain.impl.*;

import java.sql.Date;
import java.util.Calendar;
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

/**
 * Created 06/04/20
 *
 * @author Edd
 */
@ActiveProfiles("xxxx")
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

    private EntryInfoImpl mockEntryInfo(String accession, int entryVersion) {
        EntryInfoImpl entryInfo = new EntryInfoImpl();
        entryInfo.setEntryVersion(entryVersion);
        ReleaseImpl lastRelease = new ReleaseImpl();
        lastRelease.setDatabase(DatabaseEnum.Swissprot);
        lastRelease.setId(2);
        lastRelease.setReleaseNumber("2");
        lastRelease.setReleaseDate(new Date(Calendar.getInstance().getTime().getTime()));
        lastRelease.setReleaseURI("some URI");
        lastRelease.setTimeStamp(new Date(Calendar.getInstance().getTime().getTime()));

        entryInfo.setLastRelease(lastRelease);
        entryInfo.setAccession(accession);
        entryInfo.setDatabase(DatabaseEnum.Swissprot);
        entryInfo.setEntryMD5("someMd5");
        ReleaseImpl firstRelease = new ReleaseImpl();
        firstRelease.setDatabase(DatabaseEnum.Swissprot);
        firstRelease.setId(1);
        firstRelease.setReleaseNumber("1");
        firstRelease.setReleaseDate(new Date(Calendar.getInstance().getTime().getTime()));
        firstRelease.setReleaseURI("some URI");
        firstRelease.setTimeStamp(new Date(Calendar.getInstance().getTime().getTime()));
        entryInfo.setFirstRelease(firstRelease);

        return entryInfo;
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

    private EntryImpl mockEntry(String accession, int entryVersion) {
        EntryImpl entry = new EntryImpl();
        EntryContentImpl content = new EntryContentImpl();
        content.setFullcontent(
                "ID   "
                        + accession
                        + "_ID        Unreviewed;        60 AA.\n"
                        + "AC   "
                        + accession
                        + ";\n"
                        + "DT   13-FEB-2019, integrated into UniProtKB/TrEMBL.\n"
                        + "DT   13-FEB-2019, sequence version 1.\n"
                        + "DT   11-DEC-2019, entry version "
                        + entryVersion
                        + ".\n"
                        + "DE   SubName: Full=Uncharacterized protein {ECO:0000313|EMBL:AYX10384.1};\n"
                        + "GN   ORFNames=EGX52_05955 {ECO:0000313|EMBL:AYX10384.1};\n"
                        + "OS   Yersinia pseudotuberculosis.\n"
                        + "OC   Bacteria; Proteobacteria; Gammaproteobacteria; Enterobacterales;\n"
                        + "OC   Yersiniaceae; Yersinia.\n"
                        + "OX   NCBI_TaxID=633 {ECO:0000313|EMBL:AYX10384.1, ECO:0000313|Proteomes:UP000277634};\n"
                        + "RN   [1] {ECO:0000313|Proteomes:UP000277634}\n"
                        + "RP   NUCLEOTIDE SEQUENCE [LARGE SCALE GENOMIC DNA].\n"
                        + "RC   STRAIN=FDAARGOS_580 {ECO:0000313|Proteomes:UP000277634};\n"
                        + "RA   Goldberg B., Campos J., Tallon L., Sadzewicz L., Zhao X., Vavikolanu K.,\n"
                        + "RA   Mehta A., Aluvathingal J., Nadendla S., Geyer C., Nandy P., Yan Y.,\n"
                        + "RA   Sichtig H.;\n"
                        + "RT   \"FDA dAtabase for Regulatory Grade micrObial Sequences (FDA-ARGOS):\n"
                        + "RT   Supporting development and validation of Infectious Disease Dx tests.\";\n"
                        + "RL   Submitted (NOV-2018) to the EMBL/GenBank/DDBJ databases.\n"
                        + "DR   EMBL; CP033715; AYX10384.1; -; Genomic_DNA.\n"
                        + "DR   RefSeq; WP_072092108.1; NZ_PDEJ01000002.1.\n"
                        + "DR   Proteomes; UP000277634; Chromosome.\n"
                        + "PE   4: Predicted;\n"
                        + "SQ   SEQUENCE   60 AA;  6718 MW;  701D8D73381524E8 CRC64;\n"
                        + "     MASGAYSKYL FQIIGETVSS TNRGNKYNSF DHSRVDTRAG SFREAYNSKK KGSGRFGRKC\n"
                        + "//\n");
        entry.setEntryContent(content);
        entry.setEntryVersion(entryVersion);
        ReleaseImpl lastRelease = new ReleaseImpl();
        lastRelease.setDatabase(DatabaseEnum.Swissprot);
        lastRelease.setId(2);
        lastRelease.setReleaseNumber("2");
        lastRelease.setReleaseDate(new Date(Calendar.getInstance().getTime().getTime()));
        lastRelease.setReleaseURI("some URI");
        lastRelease.setTimeStamp(new Date(Calendar.getInstance().getTime().getTime()));

        entry.setLastRelease(lastRelease);
        entry.setAccession(accession);
        entry.setDatabase(DatabaseEnum.Swissprot);
        entry.setEntryMD5("someMd5");
        ReleaseImpl firstRelease = new ReleaseImpl();
        firstRelease.setDatabase(DatabaseEnum.Swissprot);
        firstRelease.setId(1);
        firstRelease.setReleaseNumber("1");
        firstRelease.setReleaseDate(new Date(Calendar.getInstance().getTime().getTime()));
        firstRelease.setReleaseURI("some URI");
        firstRelease.setTimeStamp(new Date(Calendar.getInstance().getTime().getTime()));
        entry.setFirstRelease(firstRelease);

        return entry;
    }

    @Test
    void canRetrieveSpecificVersionsOfEntriesWithoutContent() throws Exception {
        // given
        String accession = "P12345";
        when(uniSaveRepository.retrieveEntryInfo2(accession, 1))
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
        when(uniSaveRepository.retrieveEntry2(accession, 1)).thenReturn(mockEntry(accession, 1));

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
                .andExpect(jsonPath("$.messages[*]", contains("Invalid request received. Comma separated version list must only contain non-zero integers, found: XXXX")));
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

        when(uniSaveRepository.diff(accession, version1, version2)).thenReturn(diff);

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
    void diffForNonExistingEntryCauses404() throws Exception {
        // given
        String accession = "P12345";
        doThrow(ResourceNotFoundException.class).when(uniSaveRepository).diff(accession, 1, 2);

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
        DiffImpl diff = new DiffImpl();
        diff.setAccession(accession);
        diff.setDiff("mock diff");
        int version1 = 1;
        int version2 = 2;
        diff.setEntryOne(mockEntry(accession, version1));
        diff.setEntryTwo(mockEntry(accession, version2));

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
    void statusForNonExistingEntryCauses404() throws Exception {
        // given
        String accession = "P12345";
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

    @Profile("xxxx")
    @Configuration
    static class TestConfig {
        @Primary
        @Bean
        public UniSaveRepository uniSaveRepository() {
            return mock(UniSaveRepository.class);
        }
    }
}
