package uk.ac.ebi.uniprot.uuw.advanced.search.controller;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.uuw.advanced.search.AdvancedSearchREST;
import uk.ac.ebi.uniprot.uuw.advanced.search.mockers.UniProtEntryMocker;
import uk.ac.ebi.uniprot.uuw.advanced.search.repository.DataStoreManager;
import uk.ac.ebi.uniprot.uuw.advanced.search.repository.DataStoreTestConfig;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static uk.ac.ebi.uniprot.uuw.advanced.search.controller.UniprotAdvancedSearchController.UNIPROTKB_RESOURCE;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DataStoreTestConfig.class, AdvancedSearchREST.class})
@WebAppConfiguration
public class UniprotAdvancedSearchControllerIT {
    private static final String ACCESSION_RESOURCE = UNIPROTKB_RESOURCE + "/accession/";
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
    public void canReachAccessionEndpoint() throws Exception {
        // given
        String acc = saveEntry();

        // when
        ResultActions response = mockMvc.perform(
                get(ACCESSION_RESOURCE + acc)
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(content().string(containsString("\"accession\":\"" + acc + "\"")));
    }

    @Test
    public void canFilterEntryFromAccessionEndpoint() throws Exception {
        // given
        String acc = saveEntry();

        // when
        ResultActions response = mockMvc.perform(
                get(ACCESSION_RESOURCE + acc)
                        .param("fields", "gene")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.*.accession", contains(acc)))
                .andExpect(jsonPath("$.results.*.gene").exists())
                // ensure other parts of the entry were not returned (using one example)
                .andExpect(jsonPath("$.results.*.lineage").doesNotExist());
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
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(content().string(containsString("\"accession\":\"" + acc + "\"")));
    }

    @Test
    public void canFilterEntryFromSearchEndpoint() throws Exception {
        // given
        String acc = saveEntry();

        // when
        ResultActions response = mockMvc.perform(
                get(ACCESSION_RESOURCE + acc)
                        .param("query", "accession:" + acc)
                        .param("fields", "gene")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.*.accession", contains(acc)))
                .andExpect(jsonPath("$.results.*.gene").exists())
                // ensure other parts of the entry were not returned (using one example)
                .andExpect(jsonPath("$.results.*.lineage").doesNotExist());
    }

    private String saveEntry() {
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP);
        String acc = entry.getPrimaryUniProtAccession().getValue();
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);
        return acc;
    }

    @Test
    public void queryWithInvalidQueryFormat() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP);
        String acc = entry.getPrimaryUniProtAccession().getValue();
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE + "?query=invalidfield(:invalidValue AND :invalid:10)")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(content().string(containsString("query parameter has an invalid syntax")));
    }

    @Test
    public void queryWithInvalidFilterType() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP);
        String acc = entry.getPrimaryUniProtAccession().getValue();
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response = mockMvc.perform( //query={!parent which=\"accession:P21802\"}gene:BRCA2")
                get(SEARCH_RESOURCE + "?query=accession:P21802^2")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(content().string(containsString("'org.apache.lucene.search.BoostQuery' " +
                        "is currently not an accepted search type, please contact us if you require it")));
    }

    @Test
    public void queryWithInvalidFieldName() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP);
        String acc = entry.getPrimaryUniProtAccession().getValue();
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE + "?query=invalidfield:invalidValue OR invalidfield2:invalidValue2 OR accession:P21802")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(content().string(containsString("'invalidfield' is not a valid search field")))
                .andExpect(content().string(containsString("'invalidfield2' is not a valid search field")));
    }

    @Test
    public void queryWithWrongFieldType() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP);
        String acc = entry.getPrimaryUniProtAccession().getValue();
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE + "?query=accession:P21802 AND created:01-01-2018")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(content().string(containsString("'created' filter type 'term' is invalid. Expected 'range' filter type")));
    }

    @Test
    public void queryWithWrongAccessionFieldValue() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP);
        String acc = entry.getPrimaryUniProtAccession().getValue();
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE + "?query=accession:invalidValue")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(content().string(containsString("The 'accession' filter value 'invalidvalue' has invalid format. It should be a valid UniProtKB accession")));
    }

    @Test
    public void queryWithWrongProteomeFieldValue() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP);
        String acc = entry.getPrimaryUniProtAccession().getValue();
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE + "?query=proteome:invalidValue")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(content().string(containsString("The 'proteome' filter value has invalid format. It should match the regular expression UP[0-9]{9}")));
    }

    @Test
    public void queryWithWrongBooleanFieldValue() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP);
        String acc = entry.getPrimaryUniProtAccession().getValue();
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE + "?query=active:invalidValue")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(content().string(containsString("The 'active' parameter can only be true or false")));
    }

    @Test
    public void queryWithWrongNumberFieldValue() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP);
        String acc = entry.getPrimaryUniProtAccession().getValue();
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE + "?query=organism_id:invalidValue")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(content().string(containsString("The 'organism_id' filter value should be a number")));
    }

    @Test
    public void sortWithCorrectValues() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP);
        String acc = entry.getPrimaryUniProtAccession().getValue();
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE + "?query=accession:"+acc+"&sort=organism asc")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(content().string(containsString("\"accession\":\"" + acc + "\"")));
    }

    @Test
    public void sortWithMultipleCorrectValues() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP);
        String acc = entry.getPrimaryUniProtAccession().getValue();
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE + "?query=accession:"+acc+"&sort=accession desc,gene asc")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(content().string(containsString("\"accession\":\"" + acc + "\"")));
    }

    @Test
    public void sortWithIncorrectValues() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP);
        String acc = entry.getPrimaryUniProtAccession().getValue();
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE + "?query=organism_id:9606&sort=invalidField invalidSort")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(content().string(containsString("Invalid sort field invalidfield")))
                .andExpect(content().string(containsString("Invalid sort field order invalidsort. Expected asc or desc")));
    }

    @Test
    public void sortWithMultipleIncorrectValues() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP);
        String acc = entry.getPrimaryUniProtAccession().getValue();
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE + "?query=organism_id:9606&sort=invalidField invalidSort,invalidField1 invalidSort1")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(content().string(containsString("Invalid sort field order invalidsort1. Expected asc or desc")))
                .andExpect(content().string(containsString("Invalid sort field invalidfield")))
                .andExpect(content().string(containsString("Invalid sort field invalidfield1")))
                .andExpect(content().string(containsString("Invalid sort field order invalidsort. Expected asc or desc")));
    }
}