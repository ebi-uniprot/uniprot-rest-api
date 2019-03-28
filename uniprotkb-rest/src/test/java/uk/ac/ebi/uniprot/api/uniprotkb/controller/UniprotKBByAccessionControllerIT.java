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

import uk.ac.ebi.uniprot.api.common.repository.DataStoreManager;
import uk.ac.ebi.uniprot.api.common.repository.search.mockers.InactiveEntryMocker;
import uk.ac.ebi.uniprot.api.common.repository.search.mockers.UniProtEntryMocker;
import uk.ac.ebi.uniprot.api.uniprotkb.UniProtKBREST;
import uk.ac.ebi.uniprot.api.uniprotkb.repository.DataStoreTestConfig;
import uk.ac.ebi.uniprot.dataservice.source.impl.inactiveentry.InactiveUniProtEntry;
import uk.ac.ebi.uniprot.domain.uniprot.UniProtEntry;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static uk.ac.ebi.uniprot.api.uniprotkb.controller.UniprotKBController.UNIPROTKB_RESOURCE;

/**
 *
 * @author lgonzales
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DataStoreTestConfig.class, UniProtKBREST.class})
@WebAppConfiguration
public class UniprotKBByAccessionControllerIT {

    private static final String ACCESSION_RESOURCE = UNIPROTKB_RESOURCE + "/accession/";

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
    public void wrongAccessionFromAccessionEndpointReturnNotFound() throws Exception {
        // given
        String acc = saveEntry();

        // when
        ResultActions response = mockMvc.perform(
                get(ACCESSION_RESOURCE + "P05067")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.*",contains("Resource not found")));
    }

    public void invalidAccessionParametersFromAccessionEndpointReturnBadRequest() throws Exception {
        // given
        String acc = saveEntry();

        // when
        ResultActions response = mockMvc.perform(
                get(ACCESSION_RESOURCE + "invalid")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.*",
                        contains("The 'accession' value has invalid format. It should be a valid UniProtKB accession")));
    }

    @Test
    public void invalidSimpleFieldsParametersFromAccessionEndpointReturnBadRequest() throws Exception {
        // given
        String acc = saveEntry();

        // when
        ResultActions response = mockMvc.perform(
                get(ACCESSION_RESOURCE + acc)
                        .param("fields", "invalid")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.*", contains("Invalid fields parameter value 'invalid'")));
    }

    @Test
    public void invalidMultipleFieldsParametersFromAccessionEndpointReturnBadRequest() throws Exception {
        // given
        String acc = saveEntry();

        // when
        ResultActions response = mockMvc.perform(
                get(ACCESSION_RESOURCE + acc)
                        .param("fields", "invalid, organism, invalid2")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.*",
                        containsInAnyOrder("Invalid fields parameter value 'invalid'",
                                "Invalid fields parameter value 'invalid2'")));
    }

    @Test
    public void invalidAccessionAndFieldsParametersFromAccessionEndpointReturnBadRequest() throws Exception {
        // given
        String acc = saveEntry();

        // when
        ResultActions response = mockMvc.perform(
                get(ACCESSION_RESOURCE + "invalid")
                        .param("fields", "invalid")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.*",
                        containsInAnyOrder("Invalid fields parameter value 'invalid'",
                                "The 'accession' value has invalid format. It should be a valid UniProtKB accession")));
    }

    @Test
    public void canSearchAccessionFromAccessionEndpoint() throws Exception {
        // given
        String acc = saveEntry();

        // when
        ResultActions response = mockMvc.perform(
                get(ACCESSION_RESOURCE + acc)
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.primaryAccession", is(acc)));
    }

    @Test
    public void canFilterEntryFromAccessionEndpoint() throws Exception {
        // given
        String acc = saveEntry();

        // when
        ResultActions response = mockMvc.perform(
                get(ACCESSION_RESOURCE + acc)
                        .param("fields", "gene_primary")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.primaryAccession", is(acc)))
                .andExpect(jsonPath("$.genes").exists())
                // ensure other parts of the entry were not returned (using one example)
                .andExpect(jsonPath("$.organism").doesNotExist());
    }

    @Test
    public void canSearchIsoFormEntryFromAccessionEndpoint() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL_ISOFORM);
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_ISOFORM);
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);
        // when
        ResultActions response = mockMvc.perform(
                get(ACCESSION_RESOURCE + "P21802-2")
                        .param("fields", "organism")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.primaryAccession", is("P21802-2")))
                .andExpect(jsonPath("$.organism").exists())
                // ensure other parts of the entry were not returned (using one example)
                .andExpect(jsonPath("$.genes").doesNotExist());
    }

    @Test
    public void canSearchCanonicalIsoFormEntryFromAccessionEndpoint() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL_ISOFORM);
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_ISOFORM);
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);
        // when
        ResultActions response = mockMvc.perform(
                get(ACCESSION_RESOURCE + "P21802-1")
                        .param("fields", "organism")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.primaryAccession", is("P21802-1")))
                .andExpect(jsonPath("$.organism").exists())
                // ensure other parts of the entry were not returned (using one example)
                .andExpect(jsonPath("$.comments").doesNotExist());
    }

    @Test
    public void withMergedInactiveEntryReturnTheActiveOne() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        List<InactiveUniProtEntry> mergedList = InactiveEntryMocker.create(InactiveEntryMocker.InactiveType.MERGED);
        storeManager.saveEntriesInSolr(DataStoreManager.StoreType.INACTIVE_UNIPROT,mergedList);

        // when
        ResultActions response = mockMvc.perform(
                get(ACCESSION_RESOURCE + "B4DFC2")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.primaryAccession", is("P21802")));
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
                get(ACCESSION_RESOURCE + "Q00007")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.primaryAccession", is("Q00007")))
                .andExpect(jsonPath("$.entryType", is("Inactive")))
                .andExpect(jsonPath("$.inactiveReason.inactiveReasonType", is("DEMERGED")))
                .andExpect(jsonPath("$.inactiveReason.mergeDemergeTo", contains("P63150","P63151")));
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
                get(ACCESSION_RESOURCE + "I8FBX2")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.primaryAccession", is("I8FBX2")))
                .andExpect(jsonPath("$.entryType", is("Inactive")))
                .andExpect(jsonPath("$.inactiveReason.inactiveReasonType", is("DELETED")));
    }

    private String saveEntry() {
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP);
        String acc = entry.getPrimaryAccession().getValue();
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);
        return acc;
    }

}
