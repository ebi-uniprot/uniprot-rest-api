package uk.ac.ebi.uniprot.uniprotkb.controller;

import org.junit.Before;
import org.junit.Ignore;
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
import uk.ac.ebi.uniprot.common.repository.DataStoreManager;
import uk.ac.ebi.uniprot.dataservice.source.impl.inactiveentry.InactiveUniProtEntry;
import uk.ac.ebi.uniprot.domain.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.uniprotkb.UniProtKBREST;
import uk.ac.ebi.uniprot.uniprotkb.repository.DataStoreTestConfig;
import uk.ac.ebi.uniprot.uniprotkb.repository.search.mockers.InactiveEntryMocker;
import uk.ac.ebi.uniprot.uniprotkb.repository.search.mockers.UniProtEntryMocker;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static uk.ac.ebi.uniprot.uniprotkb.controller.UniprotKBController.UNIPROTKB_RESOURCE;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DataStoreTestConfig.class, UniProtKBREST.class})
@WebAppConfiguration
public class UniprotKBControllerIT {
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
                .andExpect(content().string(containsString("\"primaryAccession\":\"" + acc + "\"")));
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
                .andExpect(jsonPath("$.results.*.primaryAccession", contains(acc)))
                .andExpect(jsonPath("$.results.*.genes").exists())
                // ensure other parts of the entry were not returned (using one example)
                .andExpect(jsonPath("$.results.*.organism").doesNotExist());
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
                .andExpect(content().string(containsString("\"primaryAccession\":\"" + acc + "\"")));
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
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.*.primaryAccession", contains(acc)))
                .andExpect(jsonPath("$.results.*.genes").exists())
                // ensure other parts of the entry were not returned (using one example)
                .andExpect(jsonPath("$.results.*.organism").doesNotExist());
    }

    private String saveEntry() {
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP);
        String acc = entry.getPrimaryAccession().getValue();
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);
        return acc;
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
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(content().string(containsString("query parameter has an invalid syntax")));
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
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(content().string(containsString("'org.apache.lucene.search.BoostQuery' " +
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
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(content().string(containsString("'invalidfield' is not a valid search field")))
                .andExpect(content().string(containsString("'invalidfield2' is not a valid search field")));
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
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(content().string(containsString("'created' filter type 'term' is invalid. Expected 'range' filter type")));
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
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(content().string(containsString("The 'accession' filter value 'invalidValue' has invalid format. It should be a valid UniProtKB accession")));
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
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(content().string(containsString("The 'proteome' filter value has invalid format. It should match the regular expression UP[0-9]{9}")));
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
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(content().string(containsString("The 'active' parameter can only be true or false")));
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
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(content().string(containsString("The 'organism_id' filter value should be a number")));
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
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(content().string(containsString("\"primaryAccession\":\"" + acc + "\"")));
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
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(content().string(containsString("\"primaryAccession\":\"" + acc + "\"")));
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
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(content().string(containsString("Invalid sort field invalidfield")))
                .andExpect(content().string(containsString("Invalid sort field order invalidsort. Expected asc or desc")));
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
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(content().string(containsString("Invalid sort field order invalidsort1. Expected asc or desc")))
                .andExpect(content().string(containsString("Invalid sort field invalidfield")))
                .andExpect(content().string(containsString("Invalid sort field invalidfield1")))
                .andExpect(content().string(containsString("Invalid sort field order invalidsort. Expected asc or desc")));
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
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(content().string(containsString("Invalid fields parameter value 'invalidField'")));
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
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(content().string(containsString("Invalid fields parameter value 'invalidField'")))
                .andExpect(content().string(containsString("Invalid fields parameter value 'otherInvalid'")));
    }

    @Test
    public void searchInvalidIncludeIsoformParamterValue() throws Exception {
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
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(content().string(containsString("Invalid includeIsoform parameter value. Expected true or false")));
    }

    @Test
    public void defaultCanonicalOnly() throws Exception {
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
                get(SEARCH_RESOURCE + "?query=gene:FGFR2&fields=accession,gene_primary")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(content().string(containsString("\"primaryAccession\":\"P21802\"")))
                .andExpect(content().string(not("\"primaryAccession\":\"P21802-1\"")))
                .andExpect(content().string(not("\"primaryAccession\":\"P21802-2\"")));
    }

    @Test
    public void searchSecondaryAccession() throws Exception {
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
                get(SEARCH_RESOURCE + "?query=accession:B4DFC2&fields=accession,gene_primary")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(content().string(containsString("\"primaryAccession\":\"P21802\"")))
                .andExpect(content().string(not("\"primaryAccession\":\"P21802-1\"")))
                .andExpect(content().string(not("\"primaryAccession\":\"P21802-2\"")));
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
                get(SEARCH_RESOURCE + "?query=accession:P21802-1&fields=accession,gene_primary")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(content().string(not("\"primaryAccession\":\"P21802\"")))
                .andExpect(content().string(containsString("\"primaryAccession\":\"P21802-1\"")))
                .andExpect(content().string(not("\"primaryAccession\":\"P21802-2\"")));
    }

    @Test
    public void includeCanonicalAndIsoForm() throws Exception {
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
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(content().string(containsString("\"primaryAccession\":\"P21802\"")))
                .andExpect(content().string(not("\"primaryAccession\":\"P21802-1\"")))
                .andExpect(content().string(containsString("\"primaryAccession\":\"P21802-2\"")));
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
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(content().string(containsString("\"primaryAccession\":\"P21802\"")))
                .andExpect(content().string(not("\"primaryAccession\":\"P21802-1\"")))
                .andExpect(content().string(containsString("\"primaryAccession\":\"P21802-2\"")));
    }

    @Test
    public void isoFormOnly() throws Exception {
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
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(content().string(not("\"primaryAccession\":\"P21802\"")))
                .andExpect(content().string(not("\"primaryAccession\":\"P21802-1\"")))
                .andExpect(content().string(containsString("\"primaryAccession\":\"P21802-2\"")));
    }

    @Test
    public void canReturnFacetInformation() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_ISOFORM);
        String acc = entry.getPrimaryAccession().getValue();
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE + "?query=accession:"+acc+"&fields=accession,gene_primary&includeFacets=true")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(content().string(containsString("\"primaryAccession\":\""+acc+"\"")))
                .andExpect(content().string(containsString("\"facets\":[{\"label\":\"3D Structure\"")));
    }

    @Test
    public void canNotReturnFacetInformationForXML() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        String acc = entry.getPrimaryAccession().getValue();
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE + "?query=accession:"+acc+"&includeFacets=true")
                        .header(ACCEPT, APPLICATION_XML_VALUE));

        // then
        response.andDo(print())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_XML_VALUE))
                .andExpect(content().string(containsString("facets are only supported by application/json format")));
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
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(content().string(containsString("\"primaryAccession\":\"Q8DIA7\"")))
                .andExpect(content().string(containsString("\"organism\":{" +
                        "\"scientificName\":\"Thermosynechococcus elongatus (strain BP-1)\"," +
                        "\"taxonId\":197221," +
                        "\"lineage\":[\"Bacteria\",\"Cyanobacteria\",\"Synechococcales\",\"Synechococcaceae\"," +
                        "\"Thermosynechococcus\"]}")));
    }

    @Ignore //TODO: We need to support inactive entries (We are waiting for the new model)
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
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(content().string(containsString("\"primaryAccession\":\"Q8DIA7\"")))
                .andExpect(content().string(containsString("\"organism\":{" +
                        "\"taxonomy\":197221," +
                        "\"names\":[{\"type\":\"scientific\"," +
                        "\"value\":\"Thermosynechococcus elongatus (strain BP-1)\"" +
                        "}]}")));
    }
}