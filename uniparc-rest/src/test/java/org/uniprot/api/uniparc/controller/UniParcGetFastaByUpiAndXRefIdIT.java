package org.uniprot.api.uniparc.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.uniprot.api.rest.output.UniProtMediaType.FASTA_MEDIA_TYPE_VALUE;
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
 * @author supun
 * @since 2025-12-17
 */
@Slf4j
@ContextConfiguration(classes = {UniParcDataStoreTestConfig.class, UniParcRestApplication.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(UniParcController.class)
@AutoConfigureWebClient
@ExtendWith(value = {SpringExtension.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UniParcGetFastaByUpiAndXRefIdIT {
    private static final String path = "/uniparc/{upi}/xrefs/{xref}";

    @RegisterExtension static DataStoreManager storeManager = new DataStoreManager();

    @Autowired private MockMvc mockMvc;

    @Autowired private UniParcQueryRepository repository;

    @Value("${voldemort.uniparc.cross.reference.groupSize:#{null}}")
    private Integer xrefGroupSize;

    @BeforeAll
    void initDataStore() {
        initStoreManager(storeManager, repository);

        // create 5 entries
        IntStream.rangeClosed(1, 5).forEach(i -> saveEntry(storeManager, 1, i));
    }

    @AfterAll
    void cleanUp() {
        storeManager.cleanSolr(DataStoreManager.StoreType.UNIPARC);
        storeManager.close();
    }

    @Test
    void testSuccess() throws Exception {
        // when
        String upi = "UPI0000283A04";
        String xRef = "P12304";
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(path, upi, xRef)
                                .header(HttpHeaders.ACCEPT, FASTA_MEDIA_TYPE_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, FASTA_MEDIA_TYPE_VALUE))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                ">UPI0000283A04 anotherProteinName04 OS=Name 9606 OX=9606 AC=P12304 SS=WP_168893204 PC=UP000005640:chromosome\n"
                                                        + "MLMPKRTKYRAAAA")));
    }

    @Test
    void test_invalidUpi() throws Exception {
        // when
        String upi = "UPI0000283A09";
        String xRef = "P12304";
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(path, upi, xRef)
                                .header(HttpHeaders.ACCEPT, FASTA_MEDIA_TYPE_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, FASTA_MEDIA_TYPE_VALUE))
                .andExpect(content().string(containsString("Resource not found")));
    }

    @Test
    void test_invalidXref() throws Exception {
        // when
        String upi = "UPI0000283A04";
        String xRef = "P12309";
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(path, upi, xRef)
                                .header(HttpHeaders.ACCEPT, FASTA_MEDIA_TYPE_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, FASTA_MEDIA_TYPE_VALUE))
                .andExpect(content().string(containsString("Resource not found")));
    }

    @Test
    void testJsonFormatIsNotSupported() throws Exception {
        // when
        String upi = "UPI0000283A04";
        String xRef = "P12304";
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(path, upi, xRef)
                                .header(HttpHeaders.ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages[0]",
                                containsString(
                                        "Invalid request received. Requested media type/format not accepted: 'application/json'.")));
    }
}
