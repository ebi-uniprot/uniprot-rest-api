package org.uniprot.api.idmapping.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.mock;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
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
import org.uniprot.api.idmapping.IDMappingREST;
import org.uniprot.api.idmapping.model.IdMappingResult;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.api.idmapping.service.IDMappingPIRService;
import org.uniprot.api.rest.controller.AbstractStreamControllerIT;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.uniprotkb.UniProtKBEntryType;
import org.uniprot.core.uniprotkb.impl.UniProtKBEntryBuilder;
import org.uniprot.cv.chebi.ChebiRepo;
import org.uniprot.cv.ec.ECRepo;
import org.uniprot.cv.go.GORepo;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.indexer.uniprot.mockers.PathwayRepoMocker;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.indexer.uniprot.mockers.UniProtEntryMocker;
import org.uniprot.store.indexer.uniprotkb.converter.UniProtEntryConverter;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.DocumentConverter;
import org.uniprot.store.search.document.uniparc.UniParcDocument;
import org.uniprot.store.search.document.uniprot.UniProtDocument;

/**
 * @author sahmad
 * @created 18/02/2021
 */
@ActiveProfiles(profiles = "offline")
@ContextConfiguration(classes = {DataStoreTestConfig.class, IDMappingREST.class})
@WebMvcTest(UniProtKBIdMappingController.class)
@AutoConfigureWebClient
@ExtendWith(value = {SpringExtension.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UniProtKBIdMappingControllerIT extends AbstractStreamControllerIT {
    private static final String UNIPROTKB_ID_MAPPING_SEARCH = "/uniprotkb/idmapping/search";

    @Autowired private UniProtStoreClient<UniProtKBEntry> storeClient;
    @Autowired private IDMappingPIRService pirService;

    @Autowired private MockMvc mockMvc;
    private final UniProtEntryConverter documentConverter =
            new UniProtEntryConverter(
                    TaxonomyRepoMocker.getTaxonomyRepo(),
                    Mockito.mock(GORepo.class),
                    PathwayRepoMocker.getPathwayRepo(),
                    mock(ChebiRepo.class),
                    mock(ECRepo.class),
                    new HashMap<>());

    @Override
    protected List<SolrCollection> getSolrCollections() {
        return List.of(SolrCollection.uniprot);
    }

    private static final UniProtKBEntry TEMPLATE_ENTRY =
            UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);

    @BeforeAll
    void saveEntriesStore() throws Exception {
        for (int i = 1; i <= 20; i++) {
            UniProtKBEntryBuilder entryBuilder = UniProtKBEntryBuilder.from(TEMPLATE_ENTRY);
            String acc = String.format("Q%05d", i);
            entryBuilder.primaryAccession(acc);
            if (i % 2 == 0) {
                entryBuilder.entryType(UniProtKBEntryType.SWISSPROT);
            } else {
                entryBuilder.entryType(UniProtKBEntryType.TREMBL);
            }

            UniProtKBEntry uniProtKBEntry = entryBuilder.build();
            storeClient.saveEntry(uniProtKBEntry);

            UniProtDocument doc = documentConverter.convert(uniProtKBEntry);
            cloudSolrClient.addBean(SolrCollection.uniprot.name(), doc);
            cloudSolrClient.commit(SolrCollection.uniprot.name());
        }
    }

    @Test
    void testUniProtKBToUniProtKBMapping() throws Exception {
        // when
        IdMappingResult pirResponse =
                IdMappingResult.builder()
                        .mappedIds(
                                List.of(
                                        new IdMappingStringPair("Q00001", "Q00001"),
                                        new IdMappingStringPair("Q00002", "Q00002")))
                        .build();
        Mockito.when(pirService.doPIRRequest(ArgumentMatchers.any())).thenReturn(pirResponse);
        ResultActions response =
                mockMvc.perform(
                        get(UNIPROTKB_ID_MAPPING_SEARCH)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("from", "ACC")
                                .param("to", "ACC")
                                .param("ids", "Q00001,Q00002"));
        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", Matchers.is(2)))
                .andExpect(jsonPath("$.results.*.from", contains("Q00001", "Q00002")))
                .andExpect(
                        jsonPath(
                                "$.results.*.entry.primaryAccession",
                                contains("Q00001", "Q00002")));
    }

    @Test
    void testUniProtKBToUniProtKBMappingOnePage() throws Exception {
        // when
        Integer defaultPageSize = 5;
        List<IdMappingStringPair> idPairs =
                IntStream.rangeClosed(1, 20)
                        .mapToObj(
                                i -> {
                                    String acc = String.format("Q%05d", i);
                                    return new IdMappingStringPair(acc, acc);
                                })
                        .collect(Collectors.toList());

        IdMappingResult pirResponse = IdMappingResult.builder().mappedIds(idPairs).build();

        Mockito.when(pirService.doPIRRequest(ArgumentMatchers.any())).thenReturn(pirResponse);
        ResultActions response =
                mockMvc.perform(
                        get(UNIPROTKB_ID_MAPPING_SEARCH)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("from", "ACC")
                                .param("to", "ACC")
                                .param(
                                        "ids",
                                        "Q00001,Q00002,Q00003,Q00004,Q00005,Q00006,"
                                                + "Q00007,Q00008,Q00009,Q00010,"
                                                + "Q00011,Q00012,Q00013,Q00014,Q00015,Q00016,Q00017,Q00018,Q00019,Q0020"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", Matchers.is(defaultPageSize)))
                .andExpect(
                        jsonPath(
                                "$.results.*.from",
                                contains("Q00001", "Q00002", "Q00003", "Q00004", "Q00005")))
                .andExpect(
                        jsonPath(
                                "$.results.*.entry.primaryAccession",
                                contains("Q00001", "Q00002", "Q00003", "Q00004", "Q00005")));
    }

    @Test
    void testUniProtKBToUniProtKBMappingWithSize() throws Exception {
        // when
        Integer size = 10;
        List<IdMappingStringPair> idPairs =
                IntStream.rangeClosed(1, 20)
                        .mapToObj(
                                i -> {
                                    String acc = String.format("Q%05d", i);
                                    return new IdMappingStringPair(acc, acc);
                                })
                        .collect(Collectors.toList());

        IdMappingResult pirResponse = IdMappingResult.builder().mappedIds(idPairs).build();

        Mockito.when(pirService.doPIRRequest(ArgumentMatchers.any())).thenReturn(pirResponse);
        ResultActions response =
                mockMvc.perform(
                        get(UNIPROTKB_ID_MAPPING_SEARCH)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("from", "ACC")
                                .param("to", "ACC")
                                .param("size", String.valueOf(size))
                                .param(
                                        "ids",
                                        "Q00001,Q00002,Q00003,Q00004,Q00005,Q00006,"
                                                + "Q00007,Q00008,Q00009,Q00010,"
                                                + "Q00011,Q00012,Q00013,Q00014,Q00015,Q00016,Q00017,Q00018,Q00019,Q0020"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", Matchers.is(size)))
                .andExpect(
                        jsonPath(
                                "$.results.*.from",
                                contains(
                                        "Q00001", "Q00002", "Q00003", "Q00004", "Q00005", "Q00006",
                                        "Q00007", "Q00008", "Q00009", "Q00010")))
                .andExpect(
                        jsonPath(
                                "$.results.*.entry.primaryAccession",
                                contains(
                                        "Q00001", "Q00002", "Q00003", "Q00004", "Q00005", "Q00006",
                                        "Q00007", "Q00008", "Q00009", "Q00010")));
    }

    @Test
    void testUniProtKBToUniProtKBMappingWithSizeAndPagination() throws Exception {
        // when
        Integer size = 10;
        List<IdMappingStringPair> idPairs =
                IntStream.rangeClosed(1, 20)
                        .mapToObj(
                                i -> {
                                    String acc = String.format("Q%05d", i);
                                    return new IdMappingStringPair(acc, acc);
                                })
                        .collect(Collectors.toList());

        IdMappingResult pirResponse = IdMappingResult.builder().mappedIds(idPairs).build();

        Mockito.when(pirService.doPIRRequest(ArgumentMatchers.any())).thenReturn(pirResponse);
        ResultActions response =
                mockMvc.perform(
                        get(UNIPROTKB_ID_MAPPING_SEARCH)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("from", "ACC")
                                .param("to", "ACC")
                                .param("size", String.valueOf(size))
                                .param(
                                        "ids",
                                        "Q00001,Q00002,Q00003,Q00004,Q00005,Q00006,"
                                                + "Q00007,Q00008,Q00009,Q00010,"
                                                + "Q00011,Q00012,Q00013,Q00014,Q00015,Q00016,Q00017,Q00018,Q00019,Q0020"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string("X-TotalRecords", "20"))
                .andExpect(jsonPath("$.results.size()", Matchers.is(size)))
                .andExpect(
                        jsonPath(
                                "$.results.*.from",
                                contains(
                                        "Q00001", "Q00002", "Q00003", "Q00004", "Q00005", "Q00006",
                                        "Q00007", "Q00008", "Q00009", "Q00010")))
                .andExpect(
                        jsonPath(
                                "$.results.*.entry.primaryAccession",
                                contains(
                                        "Q00001", "Q00002", "Q00003", "Q00004", "Q00005", "Q00006",
                                        "Q00007", "Q00008", "Q00009", "Q00010")));

        String linkHeader = response.andReturn().getResponse().getHeader(HttpHeaders.LINK);
        assertThat(linkHeader, notNullValue());
        String cursor = linkHeader.split("\\?")[1].split("&")[3].split("=")[1];

        // when 2nd page
        response =
                mockMvc.perform(
                        get(UNIPROTKB_ID_MAPPING_SEARCH)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("from", "ACC")
                                .param("to", "ACC")
                                .param("size", String.valueOf(size))
                                .param(
                                        "ids",
                                        "Q00001,Q00002,Q00003,Q00004,Q00005,Q00006,"
                                                + "Q00007,Q00008,Q00009,Q00010,"
                                                + "Q00011,Q00012,Q00013,Q00014,Q00015,Q00016,Q00017,Q00018,Q00019,Q0020")
                                .param("cursor", cursor)
                                .param("size", String.valueOf(size)));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string("X-TotalRecords", "20"))
                .andExpect(header().string(HttpHeaders.LINK, nullValue()))
                .andExpect(jsonPath("$.results.size()", Matchers.is(size)))
                .andExpect(
                        jsonPath(
                                "$.results.*.from",
                                contains(
                                        "Q00011", "Q00012", "Q00013", "Q00014", "Q00015", "Q00016",
                                        "Q00017", "Q00018", "Q00019", "Q00020")))
                .andExpect(
                        jsonPath(
                                "$.results.*.entry.primaryAccession",
                                contains(
                                        "Q00011", "Q00012", "Q00013", "Q00014", "Q00015", "Q00016",
                                        "Q00017", "Q00018", "Q00019", "Q00020")));
    }

    @Test
    void testUniProtKBToUniProtKBMappingWithZeroSize() throws Exception {
        // when
        IdMappingResult pirResponse =
                IdMappingResult.builder()
                        .mappedIds(
                                List.of(
                                        new IdMappingStringPair("Q00001", "Q00001"),
                                        new IdMappingStringPair("Q00002", "Q00002")))
                        .build();
        Mockito.when(pirService.doPIRRequest(ArgumentMatchers.any())).thenReturn(pirResponse);
        ResultActions response =
                mockMvc.perform(
                        get(UNIPROTKB_ID_MAPPING_SEARCH)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("from", "ACC")
                                .param("to", "ACC")
                                .param("size", "0")
                                .param("ids", "Q00001,Q00002"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", Matchers.is(0)));
    }

    @Test
    void testUniProtKBToUniProtKBMappingWithNegativeSize() throws Exception {
        // when
        IdMappingResult pirResponse =
                IdMappingResult.builder()
                        .mappedIds(
                                List.of(
                                        new IdMappingStringPair("Q00001", "Q00001"),
                                        new IdMappingStringPair("Q00002", "Q00002")))
                        .build();
        Mockito.when(pirService.doPIRRequest(ArgumentMatchers.any())).thenReturn(pirResponse);
        ResultActions response =
                mockMvc.perform(
                        get(UNIPROTKB_ID_MAPPING_SEARCH)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("from", "ACC")
                                .param("to", "ACC")
                                .param("size", "-1")
                                .param("ids", "Q00001,Q00002"));
        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains("'size' must be greater than or equal to 0")));
    }

    @Test
    void testUniProtKBToUniProtKBMappingWithMoreThan500Size() throws Exception {
        // when
        IdMappingResult pirResponse =
                IdMappingResult.builder()
                        .mappedIds(
                                List.of(
                                        new IdMappingStringPair("Q00001", "Q00001"),
                                        new IdMappingStringPair("Q00002", "Q00002")))
                        .build();
        Mockito.when(pirService.doPIRRequest(ArgumentMatchers.any())).thenReturn(pirResponse);
        ResultActions response =
                mockMvc.perform(
                        get(UNIPROTKB_ID_MAPPING_SEARCH)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("from", "ACC")
                                .param("to", "ACC")
                                .param("size", "600")
                                .param("ids", "Q00001,Q00002"));
        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains("'size' must be less than or equal to 500")));
    }

    @Test
    void testUniProtKBToUniProtKBMappingWithFacet() throws Exception {
        // when
        IdMappingResult pirResponse =
                IdMappingResult.builder()
                        .mappedIds(
                                List.of(
                                        new IdMappingStringPair("Q00001", "Q00001"),
                                        new IdMappingStringPair("Q00002", "Q00002")))
                        .build();
        Mockito.when(pirService.doPIRRequest(ArgumentMatchers.any())).thenReturn(pirResponse);
        ResultActions response =
                mockMvc.perform(
                        get(UNIPROTKB_ID_MAPPING_SEARCH)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("from", "ACC")
                                .param("to", "ACC")
                                .param("facets","proteins_with,reviewed")
                                .param("ids", "Q00001,Q00002"));
        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", Matchers.is(2)))
                .andExpect(jsonPath("$.results.*.from", contains("Q00001", "Q00002")))
                .andExpect(
                        jsonPath(
                                "$.results.*.entry.primaryAccession",
                                contains("Q00001", "Q00002")));
    }

    @Test
    void testUniProtKBToUniProtKBMappingWithFacetFilter() throws Exception {
        // when
        IdMappingResult pirResponse =
                IdMappingResult.builder()
                        .mappedIds(
                                List.of(
                                        new IdMappingStringPair("Q00001", "Q00001"),
                                        new IdMappingStringPair("Q00002", "Q00002")))
                        .build();
        Mockito.when(pirService.doPIRRequest(ArgumentMatchers.any())).thenReturn(pirResponse);
        ResultActions response =
                mockMvc.perform(
                        get(UNIPROTKB_ID_MAPPING_SEARCH)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("from", "ACC")
                                .param("to", "ACC")
                                .param("facets","proteins_with,reviewed")
                                .param("query", "reviewed:true")
                                .param("ids", "Q00001,Q00002"));
        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", Matchers.is(1)))
                .andExpect(jsonPath("$.results.*.from", contains("Q00002")))
                .andExpect(
                        jsonPath(
                                "$.results.*.entry.primaryAccession",
                                contains("Q00002")));
    }
}
