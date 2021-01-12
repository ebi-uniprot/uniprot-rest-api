package org.uniprot.api.uniprotkb.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
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
import org.uniprot.api.rest.controller.AbstractStreamControllerIT;
import org.uniprot.api.uniprotkb.UniProtKBREST;
import org.uniprot.api.uniprotkb.repository.DataStoreTestConfig;
import org.uniprot.core.uniprotkb.ProteinExistence;
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
import org.uniprot.store.search.document.uniprot.UniProtDocument;

/**
 * @author sahmad
 * @since 2019-07-26
 */
@Slf4j
@ContextConfiguration(classes = {DataStoreTestConfig.class, UniProtKBREST.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(UniProtKBPublicationController.class)
@AutoConfigureWebClient
@ExtendWith(value = {SpringExtension.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UniProtKBGetByAccessionsWithFacetFilterIT extends AbstractStreamControllerIT {

    private static final String accessionsByIdPath = "/uniprotkb/accessions";

    private static final UniProtKBEntry TEMPLATE_ENTRY =
            UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);

    private final UniProtEntryConverter documentConverter =
            new UniProtEntryConverter(
                    TaxonomyRepoMocker.getTaxonomyRepo(),
                    Mockito.mock(GORepo.class),
                    PathwayRepoMocker.getPathwayRepo(),
                    mock(ChebiRepo.class),
                    mock(ECRepo.class),
                    new HashMap<>());

    @Autowired private UniProtStoreClient<UniProtKBEntry> storeClient;

    @Autowired private MockMvc mockMvc;

    @BeforeAll
    void saveEntriesInSolrAndStore() throws Exception {
        for (int i = 11; i <= 20; i++) {
            UniProtKBEntryBuilder entryBuilder = UniProtKBEntryBuilder.from(TEMPLATE_ENTRY);
            String acc = String.format("P%05d", i);
            entryBuilder.primaryAccession(acc);
            if (i % 2 == 0) {
                entryBuilder.entryType(UniProtKBEntryType.SWISSPROT);
            } else {
                entryBuilder.entryType(UniProtKBEntryType.TREMBL);
            }

            if (i % 2 == 0) {
                entryBuilder.proteinExistence(ProteinExistence.PROTEIN_LEVEL);
            } else if (i % 3 == 0) {
                entryBuilder.proteinExistence(ProteinExistence.TRANSCRIPT_LEVEL);
            } else {
                entryBuilder.proteinExistence(ProteinExistence.HOMOLOGY);
            }

            UniProtKBEntry uniProtKBEntry = entryBuilder.build();

            UniProtDocument convert = documentConverter.convert(uniProtKBEntry);

            cloudSolrClient.addBean(SolrCollection.uniprot.name(), convert);
            storeClient.saveEntry(uniProtKBEntry);
        }
        cloudSolrClient.commit(SolrCollection.uniprot.name());
    }

    @Test
    void getByAccessionsWithSingleValueFacetFilterSuccess() throws Exception {
        String facetFilter = "reviewed:true";
        // when
        ResultActions response =
                mockMvc.perform(
                        get(accessionsByIdPath)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param(
                                        "accessions",
                                        "P00013,P00012,P00011,P00017,P00016,P00015,P00014,P00018,P00020,P00019")
                                .param("facetFilter", facetFilter)
                                .param("size", "10"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(5)))
                .andExpect(
                        jsonPath(
                                "$.results.*.primaryAccession",
                                contains("P00012", "P00014", "P00016", "P00018", "P00020")))
                .andExpect(
                        jsonPath(
                                "$.results.*.entryType",
                                contains(
                                        "UniProtKB reviewed (Swiss-Prot)",
                                        "UniProtKB reviewed (Swiss-Prot)",
                                        "UniProtKB reviewed (Swiss-Prot)",
                                        "UniProtKB reviewed (Swiss-Prot)",
                                        "UniProtKB reviewed (Swiss-Prot)")))
                .andExpect(jsonPath("$.facets").doesNotExist());
    }

    @Test
    void getByAccessionsWithMultiValueFacetFilterSuccess() throws Exception {
        String facetFilter = "existence:HOMOLOGY OR existence:TRANSCRIPT_LEVEL";
        // when
        ResultActions response =
                mockMvc.perform(
                        get(accessionsByIdPath)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param(
                                        "accessions",
                                        "P00013,P00012,P00011,P00017,P00016,P00015,P00014,P00018,P00020,P00019")
                                .param("facetFilter", facetFilter)
                                .param("size", "10"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(5)))
                .andExpect(
                        jsonPath(
                                "$.results.*.primaryAccession",
                                contains("P00011", "P00013", "P00015", "P00017", "P00019")))
                .andExpect(
                        jsonPath(
                                "$.results.*.proteinExistence",
                                contains(
                                        "3: Inferred from homology",
                                        "3: Inferred from homology",
                                        "2: Evidence at transcript level",
                                        "3: Inferred from homology",
                                        "3: Inferred from homology")))
                .andExpect(jsonPath("$.facets").doesNotExist());
    }

    @Test
    void getByAccessionsWithSingleAndMultiValueFacetFilterSuccess() throws Exception {
        String facetFilter = "reviewed:true AND (existence:HOMOLOGY OR existence:TRANSCRIPT_LEVEL)";
        // when
        ResultActions response =
                mockMvc.perform(
                        get(accessionsByIdPath)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param(
                                        "accessions",
                                        "P00013,P00012,P00011,P00017,P00016,P00015,P00014,P00018,P00020,P00019")
                                .param("facetFilter", facetFilter)
                                .param("size", "10"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(0)));
    }

    @Test
    void getByAccessionsWithFacetsAndFacetFilterSuccess() throws Exception {
        String facetFilter = "reviewed:false";
        String facets = "existence,reviewed";
        // when
        ResultActions response =
                mockMvc.perform(
                        get(accessionsByIdPath)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param(
                                        "accessions",
                                        "P00013,P00012,P00011,P00017,P00016,P00015,P00014,P00018,P00020,P00019")
                                .param("facetFilter", facetFilter)
                                .param("facets", facets)
                                .param("fields", "accession")
                                .param("size", "10"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(5)))
                .andExpect(
                        jsonPath(
                                "$.results.*.primaryAccession",
                                contains("P00011", "P00013", "P00015", "P00017", "P00019")))
                .andExpect(jsonPath("$.facets.*.label", contains("Protein Existence", "Status")))
                .andExpect(jsonPath("$.facets.*.name", contains("existence", "reviewed")))
                .andExpect(
                        jsonPath(
                                "$.facets[0].values.*.label",
                                contains("Homology", "Transcript level")))
                .andExpect(
                        jsonPath(
                                "$.facets[0].values.*.value",
                                contains("HOMOLOGY", "TRANSCRIPT_LEVEL")))
                .andExpect(jsonPath("$.facets[0].values.*.count", contains(4, 1)))
                .andExpect(jsonPath("$.facets[1].values[0].label", equalTo("Unreviewed (TrEMBL)")))
                .andExpect(jsonPath("$.facets[1].values[0].value", equalTo("false")))
                .andExpect(jsonPath("$.facets[1].values[0].count", equalTo(5)));
    }

    @Test
    void getByAccessionsWithFacetsAndFacetFilterWithPaginationSuccess() throws Exception {
        int pageSize = 2;
        String facetFilter = "reviewed:false";
        String facets = "existence,reviewed";
        // when
        ResultActions response =
                mockMvc.perform(
                        get(accessionsByIdPath)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param(
                                        "accessions",
                                        "P00013,P00012,P00011,P00017,P00016,P00015,P00014,P00018,P00020,P00019")
                                .param("facetFilter", facetFilter)
                                .param("facets", facets)
                                .param("fields", "accession")
                                .param("size", String.valueOf(pageSize)));

        // then//, "P00019"
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string("X-TotalRecords", "5"))
                .andExpect(header().string(HttpHeaders.LINK, notNullValue()))
                .andExpect(header().string(HttpHeaders.LINK, containsString("size=2")))
                .andExpect(header().string(HttpHeaders.LINK, containsString("cursor=")))
                .andExpect(jsonPath("$.results.size()", is(pageSize)))
                .andExpect(jsonPath("$.results.*.primaryAccession", contains("P00011", "P00013")))
                .andExpect(jsonPath("$.facets.*.label", contains("Protein Existence", "Status")))
                .andExpect(jsonPath("$.facets.*.name", contains("existence", "reviewed")))
                .andExpect(
                        jsonPath(
                                "$.facets[0].values.*.label",
                                contains("Homology", "Transcript level")))
                .andExpect(
                        jsonPath(
                                "$.facets[0].values.*.value",
                                contains("HOMOLOGY", "TRANSCRIPT_LEVEL")))
                .andExpect(jsonPath("$.facets[0].values.*.count", contains(4, 1)))
                .andExpect(jsonPath("$.facets[1].values[0].label", equalTo("Unreviewed (TrEMBL)")))
                .andExpect(jsonPath("$.facets[1].values[0].value", equalTo("false")))
                .andExpect(jsonPath("$.facets[1].values[0].count", equalTo(5)));

        String linkHeader = response.andReturn().getResponse().getHeader(HttpHeaders.LINK);
        assertThat(linkHeader, notNullValue());
        String cursor = linkHeader.split("\\?")[1].split("&")[4].split("=")[1];

        // when 2nd page
        response =
                mockMvc.perform(
                        get(accessionsByIdPath)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param(
                                        "accessions",
                                        "P00013,P00012,P00011,P00017,P00016,P00015,P00014,P00018,P00020,P00019")
                                .param("facetFilter", facetFilter)
                                .param("facets", facets)
                                .param("fields", "accession")
                                .param("cursor", cursor)
                                .param("size", String.valueOf(pageSize)));

        // then 2nd page
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string("X-TotalRecords", "5"))
                .andExpect(header().string(HttpHeaders.LINK, notNullValue()))
                .andExpect(header().string(HttpHeaders.LINK, containsString("size=2")))
                .andExpect(header().string(HttpHeaders.LINK, containsString("cursor=")))
                .andExpect(jsonPath("$.results.size()", is(pageSize)))
                .andExpect(jsonPath("$.results.*.primaryAccession", contains("P00015", "P00017")))
                .andExpect(jsonPath("$.facets").doesNotExist());

        linkHeader = response.andReturn().getResponse().getHeader(HttpHeaders.LINK);
        assertThat(linkHeader, notNullValue());
        cursor = linkHeader.split("\\?")[1].split("&")[4].split("=")[1];

        // when last page
        response =
                mockMvc.perform(
                        get(accessionsByIdPath)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param(
                                        "accessions",
                                        "P00013,P00012,P00011,P00017,P00016,P00015,P00014,P00018,P00020,P00019")
                                .param("facetFilter", facetFilter)
                                .param("facets", facets)
                                .param("fields", "accession")
                                .param("cursor", cursor)
                                .param("size", String.valueOf(pageSize)));

        // then last page
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string("X-TotalRecords", "5"))
                .andExpect(header().string(HttpHeaders.LINK, nullValue()))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.results.*.primaryAccession", contains("P00019")))
                .andExpect(jsonPath("$.facets").doesNotExist());
    }

    @Test
    void getByAccessionsWithInvalidFacetNamesInFilterSuccess() throws Exception {
        String facetFilter =
                "reviewed:true AND invalidFacet1:val AND length:[1 TO 200] AND invalidFacet2:12";
        // when
        ResultActions response =
                mockMvc.perform(
                        get(accessionsByIdPath)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param(
                                        "accessions",
                                        "P00013,P00012,P00011,P00017,P00016,P00015,P00014,P00018,P00020,P00019")
                                .param("facetFilter", facetFilter)
                                .param("size", "10"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                containsInAnyOrder(
                                        "Invalid facet name 'invalidFacet1'. Expected value can be [structure_3d, proteins_with, fragment, existence, length, reviewed, annotation_score, model_organism, other_organism, proteome].",
                                        "Invalid facet name 'invalidFacet2'. Expected value can be [structure_3d, proteins_with, fragment, existence, length, reviewed, annotation_score, model_organism, other_organism, proteome].")));
    }

    @Test
    void getByAccessionsWithInvalidFacetFilterQuerySuccess() throws Exception {
        String facetFilter = "reviewed:true AND length:[1 TO 200";
        // when
        ResultActions response =
                mockMvc.perform(
                        get(accessionsByIdPath)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param(
                                        "accessions",
                                        "P00013,P00012,P00011,P00017,P00016,P00015,P00014,P00018,P00020,P00019")
                                .param("facetFilter", facetFilter)
                                .param("size", "10"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages.*", contains("query parameter has an invalid syntax")));
    }

    @Override
    protected List<SolrCollection> getSolrCollections() {
        return Collections.singletonList(SolrCollection.uniprot);
    }
}
