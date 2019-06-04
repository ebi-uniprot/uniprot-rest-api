package uk.ac.ebi.uniprot.api.uniprotkb.controller;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import uk.ac.ebi.uniprot.api.uniprotkb.UniProtKBREST;
import uk.ac.ebi.uniprot.api.uniprotkb.repository.DataStoreTestConfig;
import uk.ac.ebi.uniprot.domain.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.indexer.DataStoreManager;
import uk.ac.ebi.uniprot.indexer.uniprot.inactiveentry.InactiveUniProtEntry;
import uk.ac.ebi.uniprot.indexer.uniprot.mockers.InactiveEntryMocker;
import uk.ac.ebi.uniprot.indexer.uniprot.mockers.UniProtEntryMocker;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static uk.ac.ebi.uniprot.api.uniprotkb.controller.UniprotKBController.UNIPROTKB_RESOURCE;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DataStoreTestConfig.class, UniProtKBREST.class})
@WebAppConfiguration
public class UniprotKBSearchControllerIT {

    private static final String SEARCH_RESOURCE = UNIPROTKB_RESOURCE + "/search";

    @Autowired
    private DataStoreManager storeManager;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.
                webAppContextSetup(webApplicationContext)
                .build();
    }

    @Test
    public void canReachSearchEndpoint() throws Exception {
        // given
        String acc = saveEntry();

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE)
                        .header(ACCEPT, APPLICATION_JSON_VALUE)
                        .param("query", "accession:" + acc));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.*.primaryAccession", contains(acc)));
    }

    @Test
    public void canFilterEntryFromSearchEndpoint() throws Exception {
        // given
        String acc = saveEntry();

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE)
                        .param("query", "accession:" + acc)
                        .param("fields", "gene_names")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.*.primaryAccession", contains(acc)))
                .andExpect(jsonPath("$.results.*.genes").exists())
                // ensure other parts of the entry were not returned (using one example)
                .andExpect(jsonPath("$.results.*.organism").doesNotExist());
    }

    @Test
    public void queryIsRequired() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP);
        String acc = entry.getPrimaryAccession().getValue();
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE)
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.*",contains("'query' is a required parameter")));
    }

    @Test
    public void allowQueryAllDocuments() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP);
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE+"?query=*:*")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.*.primaryAccession").exists());
    }

    @Test
    public void allowWildcardQueryAllDocuments() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE+"?query=cc_catalytic_activity:*")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.*.primaryAccession").exists());
    }

    @Test
    public void queryWithInvalidQueryFormat() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP);
        String acc = entry.getPrimaryAccession().getValue();
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE + "?query=invalidfield(:invalidValue AND :invalid:10)")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.*",contains("query parameter has an invalid syntax")));
    }

    @Test
    public void queryWithInvalidFilterType() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP);
        String acc = entry.getPrimaryAccession().getValue();
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response = mockMvc.perform( //query={!parent which=\"accession:P21802\"}gene:BRCA2")
                get(SEARCH_RESOURCE + "?query=accession:P21802^2")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.*",contains("'org.apache.lucene.search.BoostQuery' " +
                        "is currently not an accepted search type, please contact us if you require it")));
    }

    @Test
    public void queryWithInvalidFieldName() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP);
        String acc = entry.getPrimaryAccession().getValue();
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE + "?query=invalidfield:invalidValue OR invalidfield2:invalidValue2 OR accession:P21802")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.*",
                        containsInAnyOrder("'invalidfield' is not a valid search field",
                                "'invalidfield2' is not a valid search field")));
    }

    @Test
    public void queryWithWrongFieldType() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP);
        String acc = entry.getPrimaryAccession().getValue();
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE + "?query=accession:P21802 AND created:01-01-2018")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.*",contains("'created' filter type 'term' is invalid. Expected 'range' filter type")));
    }

    @Test
    public void queryWithWrongAccessionFieldValue() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP);
        String acc = entry.getPrimaryAccession().getValue();
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE + "?query=accession:invalidValue")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.*",contains("The 'accession' filter value 'invalidValue' has invalid format. It should be a valid UniProtKB accession")));
    }

    @Test
    public void queryWithWrongProteomeFieldValue() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP);
        String acc = entry.getPrimaryAccession().getValue();
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE + "?query=proteome:invalidValue")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.*",contains("The 'proteome' filter value has invalid format. It should match the regular expression UP[0-9]{9}")));
    }

    @Test
    public void queryWithWrongBooleanFieldValue() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP);
        String acc = entry.getPrimaryAccession().getValue();
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE + "?query=active:invalidValue")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.*",contains("The 'active' parameter can only be true or false")));
    }

    @Test
    public void queryWithWrongNumberFieldValue() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP);
        String acc = entry.getPrimaryAccession().getValue();
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE + "?query=organism_id:invalidValue")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.*",contains("The 'organism_id' filter value should be a number")));
    }

    @Test
    public void sortWithCorrectValues() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP);
        String acc = entry.getPrimaryAccession().getValue();
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE + "?query=accession:"+acc+"&sort=organism asc")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.*.primaryAccession", contains(acc)));
    }

    @Test
    public void sortWithMultipleCorrectValues() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP);
        String acc = entry.getPrimaryAccession().getValue();
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE + "?query=accession:"+acc+"&sort=accession desc,gene asc")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.*.primaryAccession",contains(acc)));
    }

    @Test
    public void sortWithIncorrectValues() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP);
        String acc = entry.getPrimaryAccession().getValue();
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE + "?query=organism_id:9606&sort=invalidField invalidSort")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.*",containsInAnyOrder("Invalid sort field 'invalidfield'"
                        ,"Invalid sort field order 'invalidsort'. Expected asc or desc")));
    }

    @Test
    public void sortWithMultipleIncorrectValues() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP);
        String acc = entry.getPrimaryAccession().getValue();
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE + "?query=organism_id:9606&sort=invalidField invalidSort,invalidField1 invalidSort1")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.*",
                        containsInAnyOrder("Invalid sort field order 'invalidsort1'. Expected asc or desc",
                                "Invalid sort field 'invalidfield'",
                                "Invalid sort field 'invalidfield1'",
                                "Invalid sort field order 'invalidsort'. Expected asc or desc")));
    }

    @Test
    public void returnFieldsWithCorrectValues() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP);
        String acc = entry.getPrimaryAccession().getValue();
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE + "?query=accession:"+acc+"&fields=accession,organism")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.*.primaryAccession", contains("Q8DIA7")))
                .andExpect(jsonPath("$.results.*.organism.scientificName", contains("Thermosynechococcus elongatus (strain BP-1)")))
                .andExpect(jsonPath("$.results.*.organism.taxonId",contains(197221)))
                .andExpect(jsonPath("$.results.*.organism.lineage", hasItem(hasItems("Thermosynechococcus","Synechococcaceae"))));
    }

    @Test
    public void returnFieldsWithSingleIncorrectValue() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP);
        String acc = entry.getPrimaryAccession().getValue();
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE + "?query=organism_id:9606&fields=invalidField,accession")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.*",contains("Invalid fields parameter value 'invalidField'")));
    }

    @Test
    public void returnFieldsWithMultipleIncorrectValues() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP);
        String acc = entry.getPrimaryAccession().getValue();
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE + "?query=organism_id:9606&fields=invalidField,accession,otherInvalid")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.*",
                        containsInAnyOrder("Invalid fields parameter value 'invalidField'",
                                "Invalid fields parameter value 'otherInvalid'")));
    }

    @Test
    public void searchInvalidIncludeIsoformParameterValue() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        String acc = entry.getPrimaryAccession().getValue();
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE + "?query=accession:P21802&includeIsoform=invalid")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.*",contains("Invalid includeIsoform parameter value. Expected true or false")));
    }

    @Test
    public void searchSecondaryAccession() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        String acc = entry.getPrimaryAccession().getValue();
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_ISOFORM);
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL_ISOFORM);
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE + "?query=accession:B4DFC2&fields=accession,gene_primary")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()",is(1)))
                .andExpect(jsonPath("$.results.*.primaryAccession",contains("P21802")));
    }

    @Test
    public void searchCanonicalOnly() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        String acc = entry.getPrimaryAccession().getValue();
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL_ISOFORM);
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_ISOFORM);
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE + "?query=accession:P21802&fields=accession,gene_primary")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()",is(1)))
                .andExpect(jsonPath("$.results.*.primaryAccession",contains("P21802")))
                .andExpect(jsonPath("$.results.*.primaryAccession",not("P21802-1")))
                .andExpect(jsonPath("$.results.*.primaryAccession",not("P21802-2")));
    }

    @Test
    public void searchCanonicalIsoformAccession() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        String acc = entry.getPrimaryAccession().getValue();
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL_ISOFORM);
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_ISOFORM);
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE + "?query=accession_id:P21802-1&fields=accession,gene_primary")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.*.primaryAccession",not("P21802")))
                .andExpect(jsonPath("$.results.*.primaryAccession",contains("P21802-1")))
                .andExpect(jsonPath("$.results.*.primaryAccession",not("P21802-2")));
    }

    @Test
    public void searchIncludeCanonicalAndIsoForm() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        String acc = entry.getPrimaryAccession().getValue();
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL_ISOFORM);
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_ISOFORM);
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE + "?query=gene:FGFR2&fields=accession,gene_primary&includeIsoform=true")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.*.primaryAccession",containsInAnyOrder("P21802","P21802-2")))
                .andExpect(jsonPath("$.results.*.primaryAccession",not("P21802-1")));
    }

    @Test
    public void searchByAccessionAndIncludeIsoForm() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        String acc = entry.getPrimaryAccession().getValue();
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL_ISOFORM);
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_ISOFORM);
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE + "?query=accession:P21802&fields=accession,gene_primary&includeIsoform=true")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.*.primaryAccession",containsInAnyOrder("P21802","P21802-2")))
                .andExpect(jsonPath("$.results.*.primaryAccession",not("P21802-1")));
    }

    @Test
    public void searchIsoFormOnly() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        String acc = entry.getPrimaryAccession().getValue();
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL_ISOFORM);
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_ISOFORM);
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE + "?query=((gene:FGFR2) AND (is_isoform:true))&fields=accession,gene_primary")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.*.primaryAccession",not("P21802")))
                .andExpect(jsonPath("$.results.*.primaryAccession",not("P21802-1")))
                .andExpect(jsonPath("$.results.*.primaryAccession",contains("P21802-2")));
    }

    @Test
    public void searchWithInvalidFacetRequest() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_ISOFORM);
        String acc = entry.getPrimaryAccession().getValue();
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE + "?query=accession:"+acc+"&facets=d3structure,invalid,invalid2")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.*",containsInAnyOrder("Invalid facet name 'invalid'. " +
                        "Expected value can be [d3structure, fragment, existence, reviewed, " +
                        "annotation_score, other_organism, popular_organism].",
                        "Invalid facet name 'invalid2'. Expected value can be [d3structure, fragment, existence, " +
                                "reviewed, annotation_score, other_organism, popular_organism].")));
    }

    @Test
    public void canReturnFacetInformation() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP);
        String acc = entry.getPrimaryAccession().getValue();
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE + "?query=accession:"+acc+"&fields=accession,gene_primary&facets=reviewed, d3structure ,fragment")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.*.primaryAccession",contains(acc)))
                .andExpect(jsonPath("$.facets",notNullValue()))
                .andExpect(jsonPath("$.facets",not(empty())))
                .andExpect(jsonPath("$.facets.*.name",contains("reviewed","d3structure","fragment")));
    }

    @Test
    public void canNotReturnFacetInformationForXML() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        String acc = entry.getPrimaryAccession().getValue();
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE + "?query=accession:"+acc+"&facets=reviewed")
                        .header(ACCEPT, APPLICATION_XML_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_XML_VALUE))
                .andExpect(content().string(containsString("facets are supported only for 'application/json'")));
    }

    @Test
    public void searchForMergedInactiveEntriesAlsoReturnsActiveOne() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        List<InactiveUniProtEntry> mergedList = InactiveEntryMocker.create(InactiveEntryMocker.InactiveType.MERGED);
        storeManager.saveEntriesInSolr(DataStoreManager.StoreType.INACTIVE_UNIPROT,mergedList);

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE + "?query=accession:Q14301&fields=accession,organism")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.*.primaryAccession", containsInAnyOrder("P21802","Q14301")));
    }

    @Test
    public void searchForDeMergedInactiveEntriesReturnItself() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        List<InactiveUniProtEntry> demergedList = InactiveEntryMocker.create(InactiveEntryMocker.InactiveType.DEMERGED);
        storeManager.saveEntriesInSolr(DataStoreManager.StoreType.INACTIVE_UNIPROT,demergedList);

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE + "?query=accession:Q00007&fields=accession,organism")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.*.primaryAccession", contains("Q00007")))
                .andExpect(jsonPath("$.results.*.entryType", contains("Inactive")))
                .andExpect(jsonPath("$.results.*.inactiveReason.inactiveReasonType", contains("DEMERGED")))
                .andExpect(jsonPath("$.results.*.inactiveReason.mergeDemergeTo", contains(contains("P63150","P63151"))));
    }

    @Test
    public void searchForDeletedInactiveEntriesReturnItself() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        List<InactiveUniProtEntry> deletedList = InactiveEntryMocker.create(InactiveEntryMocker.InactiveType.DELETED);
        storeManager.saveEntriesInSolr(DataStoreManager.StoreType.INACTIVE_UNIPROT,deletedList);

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE + "?query=mnemonic:I8FBX2_YERPE&fields=accession,organism")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.*.primaryAccession", contains("I8FBX2")))
                .andExpect(jsonPath("$.results.*.entryType", contains("Inactive")))
                .andExpect(jsonPath("$.results.*.inactiveReason.inactiveReasonType", contains("DELETED")));
    }

    private String saveEntry() {
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP);
        String acc = entry.getPrimaryAccession().getValue();
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);
        return acc;
    }
}