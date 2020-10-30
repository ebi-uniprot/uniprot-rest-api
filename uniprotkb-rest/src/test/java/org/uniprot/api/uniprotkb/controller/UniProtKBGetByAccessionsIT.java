package org.uniprot.api.uniprotkb.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.uniprot.api.rest.output.UniProtMediaType.XLS_MEDIA_TYPE;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.uniprotkb.UniProtKBEntryType;
import org.uniprot.core.uniprotkb.impl.UniProtKBEntryBuilder;
import org.uniprot.cv.chebi.ChebiRepo;
import org.uniprot.cv.ec.ECRepo;
import org.uniprot.store.datastore.UniProtStoreClient;
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

    private static final String accessionsByIdPath = "/uniprotkb/accessions";

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

    @Autowired private UniProtStoreClient<UniProtKBEntry> storeClient;

    @Autowired private MockMvc mockMvc;

    @BeforeAll
    void saveEntriesInSolrAndStore() throws Exception {
        for (int i = 1; i <= 10; i++) {
            UniProtKBEntryBuilder entryBuilder = UniProtKBEntryBuilder.from(TEMPLATE_ENTRY);
            String acc = String.format("P%05d", i);
            entryBuilder.primaryAccession(acc);
            if (i % 2 == 0) {
                entryBuilder.entryType(UniProtKBEntryType.SWISSPROT);
            } else {
                entryBuilder.entryType(UniProtKBEntryType.TREMBL);
            }

            UniProtKBEntry uniProtKBEntry = entryBuilder.build();

            UniProtDocument convert = documentConverter.convert(uniProtKBEntry);

            cloudSolrClient.addBean(SolrCollection.uniprot.name(), convert);
            storeClient.saveEntry(uniProtKBEntry);
        }
        cloudSolrClient.commit(SolrCollection.uniprot.name());
    }

    @Test
    void getByAccessionsSuccess() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(accessionsByIdPath)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param(
                                        "accessions",
                                        "P00003,P00002,P00001,P00007,P00006,P00005,P00004,P00008,P00010,P00009")
                                .param("size", "10"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().doesNotExist("Content-Disposition"))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(10)))
                .andExpect(
                        jsonPath(
                                "$.results.*.primaryAccession",
                                contains(
                                        "P00003", "P00002", "P00001", "P00007", "P00006", "P00005",
                                        "P00004", "P00008", "P00010", "P00009")))
                .andExpect(
                        jsonPath(
                                "$.results[0].entryType", equalTo("UniProtKB unreviewed (TrEMBL)")))
                .andExpect(jsonPath("$.results[0].uniProtkbId", equalTo("FGFR2_HUMAN")));
    }

    @Test
    void getByAccessionsWithFacetsSuccess() throws Exception {
        String facetList = "length,model_organism,reviewed";
        // when
        ResultActions response =
                mockMvc.perform(
                        get(accessionsByIdPath)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param(
                                        "accessions",
                                        "P00003,P00002,P00001,P00007,P00006,P00005,P00004,P00008,P00010,P00009")
                                .param("facets", facetList)
                                .param("size", "10"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(10)))
                .andExpect(
                        jsonPath(
                                "$.results.*.primaryAccession",
                                contains(
                                        "P00003", "P00002", "P00001", "P00007", "P00006", "P00005",
                                        "P00004", "P00008", "P00010", "P00009")))
                .andExpect(
                        jsonPath(
                                "$.facets.*.label",
                                contains("Sequence length", "Model organisms", "Status")))
                .andExpect(
                        jsonPath(
                                "$.facets.*.name",
                                contains("length", "model_organism", "reviewed")))
                .andExpect(jsonPath("$.facets[0].values.*.label", contains(">= 801")))
                .andExpect(jsonPath("$.facets[0].values.*.value", contains("[801 TO *]")))
                .andExpect(jsonPath("$.facets[0].values.*.count", contains(10)))
                .andExpect(jsonPath("$.facets[1].values.*.label").doesNotExist())
                .andExpect(jsonPath("$.facets[1].values.*.value", contains("Human")))
                .andExpect(jsonPath("$.facets[1].values.*.count", contains(10)))
                .andExpect(jsonPath("$.facets[2].values[0].label", equalTo("Unreviewed (TrEMBL)")))
                .andExpect(jsonPath("$.facets[2].values[0].value", equalTo("false")))
                .andExpect(jsonPath("$.facets[2].values[0].count", equalTo(5)))
                .andExpect(
                        jsonPath("$.facets[2].values[1].label", equalTo("Reviewed (Swiss-Prot)")))
                .andExpect(jsonPath("$.facets[2].values[1].value", equalTo("true")))
                .andExpect(jsonPath("$.facets[2].values[1].count", equalTo(5)));
    }

    @Test
    void getByAccessionsWithAllFacetsSuccess() throws Exception {
        String facetList =
                "reviewed,fragment,structure_3d,model_organism,other_organism,existence,"
                        + "annotation_score,proteome,proteins_with,length";
        // when
        ResultActions response =
                mockMvc.perform(
                        get(accessionsByIdPath)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param(
                                        "accessions",
                                        "P00003,P00002,P00001,P00007,P00006,P00005,P00004,P00008,P00010,P00009")
                                .param("facets", facetList)
                                .param("size", "10"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(10)))
                .andExpect(
                        jsonPath(
                                "$.results.*.primaryAccession",
                                contains(
                                        "P00003", "P00002", "P00001", "P00007", "P00006", "P00005",
                                        "P00004", "P00008", "P00010", "P00009")))
                .andExpect(jsonPath("$.facets.size()", is(9)))
                .andExpect(
                        jsonPath(
                                "$.facets.*.label",
                                contains(
                                        "Status",
                                        "Fragment",
                                        "3D Structure",
                                        "Model organisms",
                                        "Protein Existence",
                                        "Annotation Score",
                                        "Proteomes",
                                        "Proteins with",
                                        "Sequence length")));
    }

    @Test
    void getByAccessionsWithFacetsOnlySuccess() throws Exception {
        String facetList = "reviewed,fragment,structure_3d,model_organism";
        // when
        ResultActions response =
                mockMvc.perform(
                        get(accessionsByIdPath)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param(
                                        "accessions",
                                        "P00003,P00002,P00001,P00007,P00006,P00005,P00004,P00008,P00010,P00009")
                                .param("facets", facetList)
                                .param("size", "1"));

        String linkHeader = response.andReturn().getResponse().getHeader(HttpHeaders.LINK);
        assertThat(linkHeader, notNullValue());
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.facets.size()", is(4)))
                .andExpect(
                        jsonPath(
                                "$.facets.*.label",
                                contains("Status", "Fragment", "3D Structure", "Model organisms")));
    }

    @Test
    void getByAccessionsDownloadWorks() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(accessionsByIdPath)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("download", "true")
                                .param(
                                        "accessions",
                                        "P00001,P00002,P00003,P00007,P00006,P00005,P00004,P00008,P00010,P00009")
                                .param("size", "10"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        header().string(
                                        "Content-Disposition",
                                        startsWith(
                                                "form-data; name=\"attachment\"; filename=\"uniprot-")))
                .andExpect(jsonPath("$.results.size()", is(10)))
                .andExpect(jsonPath("$.facets").doesNotExist())
                .andExpect(
                        jsonPath(
                                "$.results.*.primaryAccession",
                                contains(
                                        "P00001", "P00002", "P00003", "P00007", "P00006", "P00005",
                                        "P00004", "P00008", "P00010", "P00009")));
    }

    @Test
    void getByAccessionsFieldsParameterWorks() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(accessionsByIdPath)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("fields", "gene_names,organism_name")
                                .param(
                                        "accessions",
                                        "P00001,P00002,P00003,P00007,P00006,P00005,P00004,P00008,P00010,P00009")
                                .param("size", "10"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.results.*.primaryAccession",
                                contains(
                                        "P00001", "P00002", "P00003", "P00007", "P00006", "P00005",
                                        "P00004", "P00008", "P00010", "P00009")))
                .andExpect(jsonPath("$.results.*.organism").exists())
                .andExpect(jsonPath("$.results.*.genes").exists())
                .andExpect(jsonPath("$.results.*.sequence").doesNotExist())
                .andExpect(jsonPath("$.results.*.comments").doesNotExist())
                .andExpect(
                        jsonPath("$.results[0].organism.scientificName", equalTo("Homo sapiens")))
                .andExpect(jsonPath("$.results[0].organism.commonName", equalTo("Human")))
                .andExpect(jsonPath("$.results[0].organism.taxonId", equalTo(9606)))
                .andExpect(
                        jsonPath(
                                "$.results[0].organism.lineage",
                                equalTo(TEMPLATE_ENTRY.getOrganism().getLineages())));
    }

    @Test
    void getByAccessionsWithPagination() throws Exception {
        int pageSize = 4;
        String facetList = "length,model_organism";
        // when
        ResultActions response =
                mockMvc.perform(
                        get(accessionsByIdPath)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("fields", "gene_names,organism_name")
                                .param("facets", facetList)
                                .param(
                                        "accessions",
                                        "P00001,P00002,P00003,P00007,P00006,P00005,P00004,P00008,P00010,P00009")
                                .param("size", String.valueOf(pageSize)));

        // then first page
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string("X-TotalRecords", "10"))
                .andExpect(header().string(HttpHeaders.LINK, notNullValue()))
                .andExpect(header().string(HttpHeaders.LINK, containsString("size=4")))
                .andExpect(header().string(HttpHeaders.LINK, containsString("cursor=")))
                .andExpect(jsonPath("$.results.size()", is(pageSize)))
                .andExpect(
                        jsonPath(
                                "$.results.*.primaryAccession",
                                contains("P00001", "P00002", "P00003", "P00007")))
                .andExpect(jsonPath("$.facets").exists())
                .andExpect(jsonPath("$.facets.size()", is(2)))
                .andExpect(
                        jsonPath(
                                "$.facets.*.label",
                                contains("Sequence length", "Model organisms")));

        String linkHeader = response.andReturn().getResponse().getHeader(HttpHeaders.LINK);
        assertThat(linkHeader, notNullValue());
        String cursor = linkHeader.split("\\?")[1].split("&")[3].split("=")[1];
        // when 2nd page
        response =
                mockMvc.perform(
                        get(accessionsByIdPath)
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param(
                                        "accessions",
                                        "P00001,P00002,P00003,P00007,P00006,P00005,P00004,P00008,P00010,P00009")
                                .param("fields", "gene_names,organism_name")
                                .param("facets", facetList)
                                .param("cursor", cursor)
                                .param("size", String.valueOf(pageSize)));

        // then 2nd page
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string("X-TotalRecords", "10"))
                .andExpect(header().string(HttpHeaders.LINK, notNullValue()))
                .andExpect(header().string(HttpHeaders.LINK, containsString("size=4")))
                .andExpect(header().string(HttpHeaders.LINK, containsString("cursor=")))
                .andExpect(jsonPath("$.results.size()", is(pageSize)))
                .andExpect(
                        jsonPath(
                                "$.results.*.primaryAccession",
                                contains("P00006", "P00005", "P00004", "P00008")))
                .andExpect(jsonPath("$.facets").doesNotExist());

        linkHeader = response.andReturn().getResponse().getHeader(HttpHeaders.LINK);
        assertThat(linkHeader, notNullValue());
        cursor = linkHeader.split("\\?")[1].split("&")[3].split("=")[1];

        // when last page
        response =
                mockMvc.perform(
                        get(accessionsByIdPath)
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param(
                                        "accessions",
                                        "P00001,P00002,P00003,P00007,P00006,P00005,P00004,P00008,P00010,P00009")
                                .param("fields", "gene_names,organism_name")
                                .param("cursor", cursor)
                                .param("size", String.valueOf(pageSize)));

        // then last page
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string("X-TotalRecords", "10"))
                .andExpect(header().string(HttpHeaders.LINK, nullValue()))
                .andExpect(jsonPath("$.results.size()", is(2)))
                .andExpect(jsonPath("$.results.*.primaryAccession", contains("P00010", "P00009")))
                .andExpect(jsonPath("$.facets").doesNotExist());
    }

    @ParameterizedTest(name = "[{index}] contentType {0}")
    @MethodSource("getContentTypes")
    void allContentTypeWorks(MediaType mediaType) throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(accessionsByIdPath)
                                .header(ACCEPT, mediaType)
                                .param(
                                        "accessions",
                                        "P00001,P00002,P00003,P00007,P00006,P00005,P00004,P00008,P00010,P00009"));

        // then
        ResultActions resultTests =
                response.andDo(log())
                        .andExpect(status().is(HttpStatus.OK.value()))
                        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, mediaType.toString()))
                        .andExpect(content().contentTypeCompatibleWith(mediaType));

        if (!mediaType.equals(XLS_MEDIA_TYPE)) { // unable to compare xls binary type
            resultTests
                    .andExpect(content().string(containsString("P00001")))
                    .andExpect(content().string(containsString("P00002")))
                    .andExpect(content().string(containsString("P00003")))
                    .andExpect(content().string(containsString("P00004")))
                    .andExpect(content().string(containsString("P00005")))
                    .andExpect(content().string(containsString("P00006")))
                    .andExpect(content().string(containsString("P00007")))
                    .andExpect(content().string(containsString("P00008")))
                    .andExpect(content().string(containsString("P00009")))
                    .andExpect(content().string(containsString("P00010")));
        }
    }

    @Test
    void getByAccessionsPostSuccess() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        post(accessionsByIdPath)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param(
                                        "accessions",
                                        "P00003,P00002,P00001,P00007,P00006,P00005,P00004,P00008,P00010,P00009")
                                .param("size", "10"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().doesNotExist("Content-Disposition"))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(10)))
                .andExpect(
                        jsonPath(
                                "$.results.*.primaryAccession",
                                contains(
                                        "P00003", "P00002", "P00001", "P00007", "P00006", "P00005",
                                        "P00004", "P00008", "P00010", "P00009")))
                .andExpect(
                        jsonPath(
                                "$.results[0].entryType", equalTo("UniProtKB unreviewed (TrEMBL)")))
                .andExpect(jsonPath("$.results[0].uniProtkbId", equalTo("FGFR2_HUMAN")));
    }

    @Test
    void getByAccessionsWithFewAccessionsMissingFromStoreWithFacetsSuccess() throws Exception {
        String facetList = "length,model_organism,reviewed";
        String missingAccessions = "Q54321,Q12345";
        // when
        ResultActions response =
                mockMvc.perform(
                        get(accessionsByIdPath)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param(
                                        "accessions",
                                        "P00003,P00002,P00001,Q54321,P00007,P00006,P00005,Q12345")
                                .param("facets", facetList)
                                .param("size", "8"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(6)))
                .andExpect(
                        jsonPath(
                                "$.results.*.primaryAccession",
                                contains(
                                        "P00003", "P00002", "P00001", "P00007", "P00006",
                                        "P00005")))
                .andExpect(
                        jsonPath(
                                "$.facets.*.label",
                                contains("Sequence length", "Model organisms", "Status")))
                .andExpect(
                        jsonPath(
                                "$.facets.*.name",
                                contains("length", "model_organism", "reviewed")))
                .andExpect(jsonPath("$.facets[0].values.*.label", contains(">= 801")))
                .andExpect(jsonPath("$.facets[0].values.*.value", contains("[801 TO *]")))
                .andExpect(jsonPath("$.facets[0].values.*.count", contains(6)))
                .andExpect(jsonPath("$.facets[1].values.*.label").doesNotExist())
                .andExpect(jsonPath("$.facets[1].values.*.value", contains("Human")))
                .andExpect(jsonPath("$.facets[1].values.*.count", contains(6)))
                .andExpect(jsonPath("$.facets[2].values[0].label", equalTo("Unreviewed (TrEMBL)")))
                .andExpect(jsonPath("$.facets[2].values[0].value", equalTo("false")))
                .andExpect(jsonPath("$.facets[2].values[0].count", equalTo(4)))
                .andExpect(
                        jsonPath("$.facets[2].values[1].label", equalTo("Reviewed (Swiss-Prot)")))
                .andExpect(jsonPath("$.facets[2].values[1].value", equalTo("true")))
                .andExpect(jsonPath("$.facets[2].values[1].count", equalTo(2)));
    }

    @Test
    void getByAccessionsWithAllAccessionsMissingFromStoreWithFacetsSuccess() throws Exception {
        String facetList = "length,model_organism,reviewed";
        String missingAccessions = "Q54321,Q12345";
        // when
        ResultActions response =
                mockMvc.perform(
                        get(accessionsByIdPath)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("accessions", missingAccessions)
                                .param("facets", facetList)
                                .param("size", "2"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(0)))
                .andExpect(jsonPath("$.facets").doesNotExist());
    }

    @Test
    void getByAccessionsBadRequest() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(accessionsByIdPath)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("download", "INVALID")
                                .param("fields", "invalid, invalid1")
                                .param(
                                        "accessions",
                                        "P10000,P20000,P30000,P40000,P50000,P60000,P70000,P80000,P90000,INVALID , INVALID2")
                                .param("size", "10"));

        // then
        response.andDo(log())
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
    void getByAccessionsWithInvalidFacets() throws Exception {
        String facetList = "length,model_organism,reviewed123";
        // when
        ResultActions response =
                mockMvc.perform(
                        get(accessionsByIdPath)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param(
                                        "accessions",
                                        "P00003,P00002,P00001,P00007,P00006,P00005,P00004,P00008,P00010,P00009")
                                .param("facets", facetList)
                                .param("size", "10"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                containsInAnyOrder(
                                        "Invalid facet name 'reviewed123'. Expected value can be [structure_3d, fragment, proteins_with, length, existence, reviewed, annotation_score, model_organism, other_organism, proteome].")));
    }

    @Test
    void getByAccessionsWithPageSizeMoreThanAccessionsSize() throws Exception {
        int pageSize = 30;
        // when
        ResultActions response =
                mockMvc.perform(
                        get(accessionsByIdPath)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("fields", "gene_names,organism_name")
                                .param(
                                        "accessions",
                                        "P00001,P00002,P00003,P00007,P00006,P00005,P00004,P00008,P00010,P00009")
                                .param("size", String.valueOf(pageSize)));

        // then first page
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string("X-TotalRecords", "10"))
                .andExpect(header().string(HttpHeaders.LINK, nullValue()))
                .andExpect(jsonPath("$.results.size()", is(10)))
                .andExpect(
                        jsonPath(
                                "$.results.*.primaryAccession",
                                contains(
                                        "P00001", "P00002", "P00003", "P00007", "P00006", "P00005",
                                        "P00004", "P00008", "P00010", "P00009")));
    }

    @Test
    void getByAccessionsWithLowercaseLettersSuccess() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(accessionsByIdPath)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param(
                                        "accessions",
                                        "p00003,p00002,P00001,p00007,P00006,P00005,P00004,p00008,P00010,p00009")
                                .param("size", "10"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().doesNotExist("Content-Disposition"))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(10)))
                .andExpect(
                        jsonPath(
                                "$.results.*.primaryAccession",
                                contains(
                                        "P00003", "P00002", "P00001", "P00007", "P00006", "P00005",
                                        "P00004", "P00008", "P00010", "P00009")))
                .andExpect(
                        jsonPath(
                                "$.results[0].entryType", equalTo("UniProtKB unreviewed (TrEMBL)")))
                .andExpect(jsonPath("$.results[0].uniProtkbId", equalTo("FGFR2_HUMAN")));
    }

    @Override
    protected List<SolrCollection> getSolrCollections() {
        return Collections.singletonList(SolrCollection.uniprot);
    }

    private Stream<Arguments> getContentTypes() {
        return super.getContentTypes(accessionsByIdPath);
    }
}
