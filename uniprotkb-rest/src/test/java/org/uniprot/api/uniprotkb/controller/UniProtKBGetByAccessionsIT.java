package org.uniprot.api.uniprotkb.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.uniprot.api.rest.controller.AbstractStreamControllerIT;
import org.uniprot.api.uniprotkb.UniProtKBREST;
import org.uniprot.api.uniprotkb.repository.DataStoreTestConfig;
import org.uniprot.api.uniprotkb.repository.store.UniProtKBStoreClient;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.uniprotkb.impl.UniProtKBEntryBuilder;
import org.uniprot.cv.chebi.ChebiRepo;
import org.uniprot.cv.ec.ECRepo;
import org.uniprot.store.indexer.uniprot.mockers.GoRelationsRepoMocker;
import org.uniprot.store.indexer.uniprot.mockers.PathwayRepoMocker;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.indexer.uniprot.mockers.UniProtEntryMocker;
import org.uniprot.store.indexer.uniprotkb.converter.UniProtEntryConverter;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.uniprot.UniProtDocument;

/**
 * @author lgonzales
 * @since 2019-07-10
 */
@Slf4j
@ContextConfiguration(classes = {DataStoreTestConfig.class, UniProtKBREST.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(UniProtKBEntryController.class)
@AutoConfigureWebClient
@ExtendWith(value = {SpringExtension.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UniProtKBGetByAccessionsIT extends AbstractStreamControllerIT {

    private static final UniProtKBEntry TEMPLATE_ENTRY =
            UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);

    private final UniProtEntryConverter documentConverter =
            new UniProtEntryConverter(
                    TaxonomyRepoMocker.getTaxonomyRepo(),
                    GoRelationsRepoMocker.getGoRelationRepo(),
                    PathwayRepoMocker.getPathwayRepo(),
                    mock(ChebiRepo.class),
                    mock(ECRepo.class),
                    new HashMap<>());

    @Autowired private UniProtKBStoreClient storeClient;

    @Autowired private MockMvc mockMvc;

    @BeforeAll
    void saveEntriesInSolrAndStore() throws Exception {
        for (int i = 1; i <= 10; i++) {
            UniProtKBEntryBuilder entryBuilder = UniProtKBEntryBuilder.from(TEMPLATE_ENTRY);
            String acc = String.format("P%05d", i);
            entryBuilder.primaryAccession(acc);

            UniProtKBEntry uniProtKBEntry = entryBuilder.build();
            UniProtDocument convert = documentConverter.convert(uniProtKBEntry);

            cloudSolrClient.addBean(SolrCollection.uniprot.name(), convert);
            storeClient.saveEntry(uniProtKBEntry);
        }
        cloudSolrClient.commit(SolrCollection.uniprot.name());
    }

    @Test
    void getByAccessionsBadRequest() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get("/uniprotkb/accessions")
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("download", "INVALID")
                                .param("fields", "invalid, invalid1")
                                .param(
                                        "accessions",
                                        "P10000,P20000,P30000,P40000,P50000,P60000,P70000,P80000,P90000,INVALID , INVALID2")
                                .param("size", "10"));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                containsInAnyOrder(
                                        "Only '10' accessions are allowed in each request.",
                                        "Invalid fields parameter value 'invalid'",
                                        "Invalid fields parameter value 'invalid1'",
                                        "The 'download' parameter has invalid format. It should be a boolean true or false.",
                                        "Accession 'INVALID' has invalid format. It should be a valid UniProtKB accession.",
                                        "Accession 'INVALID2' has invalid format. It should be a valid UniProtKB accession.")));
    }

    @Test
    void getByAccessionsSuccess() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get("/uniprotkb/accessions")
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param(
                                        "accessions",
                                        "P00003,P00002,P00001,P00007,P00006,P00005,P00004,P00008,P00010,P00009")
                                .param("size", "10"));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(10)))
                .andExpect(
                        jsonPath(
                                "$.results.*.primaryAccession",
                                contains(
                                        "P00003", "P00002", "P00001", "P00007", "P00006", "P00005",
                                        "P00004", "P00008", "P00010", "P00009")));
        Assertions.fail("TODO: ADD VALIDATIONS");
    }

    @Test
    void getByAccessionsFieldsParameterWorks() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get("/uniprotkb/accessions")
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("fields", "gene_names,organism_name")
                                .param(
                                        "accessions",
                                        "P00001,P00002,P00003,P00007,P00006,P00005,P00004,P00008,P00010,P00009")
                                .param("size", "10"));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.results.*.primaryAccession",
                                contains(
                                        "P00001", "P00002", "P00003", "P00007", "P00006", "P00005",
                                        "P00004", "P00008", "P00010", "P00009")))
                .andExpect(jsonPath("$.results.*.organism").exists()) // @Shadab: check value?
                .andExpect(jsonPath("$.results.*.genes").exists()) // / @Shadab: check value?
                .andExpect(jsonPath("$.results.*.sequence").doesNotExist())
                .andExpect(jsonPath("$.results.*.comments").doesNotExist());
        Assertions.fail("TODO: Check questions above");
    }

    @Test
    void getByAccessionsDownloadWorks() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get("/uniprotkb/accessions")
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("download", "true")
                                .param(
                                        "accessions",
                                        "P00001,P00002,P00003,P00007,P00006,P00005,P00004,P00008,P00010,P00009")
                                .param("size", "10"));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        header().string(
                                        "Content-Disposition",
                                        startsWith(
                                                "form-data; name=\"attachment\"; filename=\"uniprot-")))
                .andExpect(jsonPath("$.results.size()", is(10)))
                .andExpect(
                        jsonPath(
                                "$.results.*.primaryAccession",
                                contains(
                                        "P00001", "P00002", "P00003", "P00007", "P00006", "P00005",
                                        "P00004", "P00008", "P00010", "P00009")));
        Assertions.fail("TODO: ADD VALIDATIONS");
    }

    @Override
    protected List<SolrCollection> getSolrCollections() {
        return Collections.singletonList(SolrCollection.uniprot);
    }
}
