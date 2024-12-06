package org.uniprot.api.uniparc.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.uniprot.api.rest.output.UniProtMediaType.*;
import static org.uniprot.api.rest.output.header.HttpCommonHeaderConfig.X_TOTAL_RESULTS;
import static org.uniprot.api.uniparc.controller.UniParcITUtils.*;

import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.uniprot.api.uniparc.UniParcRestApplication;
import org.uniprot.api.uniparc.common.repository.UniParcDataStoreTestConfig;
import org.uniprot.api.uniparc.common.repository.search.UniParcQueryRepository;
import org.uniprot.store.indexer.DataStoreManager;

import lombok.extern.slf4j.Slf4j;

/**
 * @author sahmad
 * @since 2020-08-11
 */
@Slf4j
@ContextConfiguration(classes = {UniParcDataStoreTestConfig.class, UniParcRestApplication.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(UniParcController.class)
@AutoConfigureWebClient
@ExtendWith(value = {SpringExtension.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UniParcGetFastaByProteomeIdIT {
    private static final String getByUpIdPath = "/uniparc/proteome/{upid}";

    @RegisterExtension static DataStoreManager storeManager = new DataStoreManager();

    @Autowired private MockMvc mockMvc;

    @Autowired private UniParcQueryRepository repository;

    @Value("${voldemort.uniparc.cross.reference.groupSize:#{null}}")
    private Integer xrefGroupSize;

    @BeforeAll
    void initDataStore() {
        initStoreManager(storeManager, repository);

        // create 5 entries
        IntStream.rangeClosed(1, 5).forEach(i -> saveEntry(storeManager, xrefGroupSize, i));
    }

    @AfterAll
    void cleanUp() {
        storeManager.cleanSolr(DataStoreManager.StoreType.UNIPARC);
        storeManager.close();
    }

    @Test
    void testGetByUpIdSuccess() throws Exception {
        // when
        String upid = "UP000005640";
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getByUpIdPath, upid)
                                .header(HttpHeaders.ACCEPT, FASTA_MEDIA_TYPE_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, FASTA_MEDIA_TYPE_VALUE))
                .andExpect(header().string(X_TOTAL_RESULTS, "5"))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                ">UPI0000283A01 anotherProteinName01 OS=Name 9606 OX=9606 AC=P12301 SS=WP_168893201 PC=UP000005640:chromosome\n"
                                                        + "MLMPKRTKYRA")))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                ">UPI0000283A02 anotherProteinName02 OS=Name 9606 OX=9606 AC=P12302 SS=WP_168893202 PC=UP000005640:chromosome\n"
                                                        + "MLMPKRTKYRAA")))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                ">UPI0000283A03 anotherProteinName03 OS=Name 9606 OX=9606 AC=P12303 SS=WP_168893203 PC=UP000005640:chromosome\n"
                                                        + "MLMPKRTKYRAAA")))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                ">UPI0000283A04 anotherProteinName04 OS=Name 9606 OX=9606 AC=P12304 SS=WP_168893204 PC=UP000005640:chromosome\n"
                                                        + "MLMPKRTKYRAAAA")))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                ">UPI0000283A05 anotherProteinName05 OS=Name 9606 OX=9606 AC=P12305 SS=WP_168893205 PC=UP000005640:chromosome\n"
                                                        + "MLMPKRTKYRAAAAA")));
    }

    @Test
    void testGetByProteomeIdWithPagination() throws Exception {
        // when
        String upid = "UP000005640";
        int size = 2;
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getByUpIdPath, upid)
                                .param("size", String.valueOf(size))
                                .header(HttpHeaders.ACCEPT, FASTA_MEDIA_TYPE_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, FASTA_MEDIA_TYPE_VALUE))
                .andExpect(header().string(X_TOTAL_RESULTS, "5"))
                .andExpect(header().string(HttpHeaders.LINK, notNullValue()))
                .andExpect(header().string(HttpHeaders.LINK, containsString("size=2")))
                .andExpect(header().string(HttpHeaders.LINK, containsString("cursor=")))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                ">UPI0000283A01 anotherProteinName01 OS=Name 9606 OX=9606 AC=P12301 SS=WP_168893201 PC=UP000005640:chromosome\n"
                                                        + "MLMPKRTKYRA")))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                ">UPI0000283A02 anotherProteinName02 OS=Name 9606 OX=9606 AC=P12302 SS=WP_168893202 PC=UP000005640:chromosome\n"
                                                        + "MLMPKRTKYRAA")));

        String cursor1 = extractCursor(response, 0);
        // when get second page
        ResultActions responsePage2 =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getByUpIdPath, upid)
                                .param("size", String.valueOf(size))
                                .param("cursor", cursor1)
                                .header(HttpHeaders.ACCEPT, FASTA_MEDIA_TYPE_VALUE));

        // then verify second page
        responsePage2
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, FASTA_MEDIA_TYPE_VALUE))
                .andExpect(header().string(X_TOTAL_RESULTS, "5"))
                .andExpect(header().string(HttpHeaders.LINK, notNullValue()))
                .andExpect(header().string(HttpHeaders.LINK, containsString("size=2")))
                .andExpect(header().string(HttpHeaders.LINK, containsString("cursor=")))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                ">UPI0000283A03 anotherProteinName03 OS=Name 9606 OX=9606 AC=P12303 SS=WP_168893203 PC=UP000005640:chromosome\n"
                                                        + "MLMPKRTKYRAAA")))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                ">UPI0000283A04 anotherProteinName04 OS=Name 9606 OX=9606 AC=P12304 SS=WP_168893204 PC=UP000005640:chromosome\n"
                                                        + "MLMPKRTKYRAAAA")));

        String cursor2 = extractCursor(responsePage2, 0);

        // when get third page
        ResultActions responsePage3 =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getByUpIdPath, upid)
                                .param("size", String.valueOf(size))
                                .param("cursor", cursor2)
                                .header(HttpHeaders.ACCEPT, FASTA_MEDIA_TYPE_VALUE));
        ;

        // then verify third page
        responsePage3
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, FASTA_MEDIA_TYPE_VALUE))
                .andExpect(header().string(X_TOTAL_RESULTS, "5"))
                .andExpect(header().string(HttpHeaders.LINK, nullValue()))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                ">UPI0000283A05 anotherProteinName05 OS=Name 9606 OX=9606 AC=P12305 SS=WP_168893205 PC=UP000005640:chromosome\n"
                                                        + "MLMPKRTKYRAAAAA")));
    }

    @Test
    void testGetByNonExistingProteomeIdSuccess() throws Exception {
        // when
        String upid = "randomId";
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getByUpIdPath, upid)
                                .header(HttpHeaders.ACCEPT, FASTA_MEDIA_TYPE_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, FASTA_MEDIA_TYPE_VALUE))
                .andExpect(content().string(""));
    }

    @Test
    void testGetByProteomeJsonFormatIsNotSupported() throws Exception {
        // when
        String upid = "UP000005640";
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getByUpIdPath, upid)
                                .param("size", "2")
                                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages[0]",
                                containsString(
                                        "Invalid request received. Requested media type/format not accepted: 'application/json'.")));
    }
}
