package uk.ac.ebi.uniprot.api.taxonomy;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.ac.ebi.uniprot.api.DataStoreTestConfig;
import uk.ac.ebi.uniprot.api.support_data.SupportDataApplication;
import uk.ac.ebi.uniprot.domain.taxonomy.TaxonomyEntry;
import uk.ac.ebi.uniprot.domain.taxonomy.TaxonomyRank;
import uk.ac.ebi.uniprot.domain.taxonomy.builder.TaxonomyEntryBuilder;
import uk.ac.ebi.uniprot.indexer.DataStoreManager;
import uk.ac.ebi.uniprot.json.parser.taxonomy.TaxonomyJsonConfig;
import uk.ac.ebi.uniprot.search.document.taxonomy.TaxonomyDocument;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @author lgonzales
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes= {DataStoreTestConfig.class, SupportDataApplication.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(TaxonomyController.class)
class TaxonomyControllerIT {

    private static final String ID_QUERY_RESOURCE = "/taxonomy/";
    private static final String SEARCH_QUERY_RESOURCE = "/taxonomy/search";
    private static final String DOWNLOAD_QUERY_RESOURCE = "/taxonomy/download";

    private static final String QUERY_PARAM = "query";
    private static final String FACETS_PARAM = "facets";
    private static final String FIELDS_PARAM = "fields";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataStoreManager storeManager;


    @Test
    void wrongIdFromIdEndpointReturnNotFound() throws Exception {
        // given
        storeManager.saveDocs(DataStoreManager.StoreType.TAXONOMY,createDocument(10L,false));

        // when
        ResultActions response = mockMvc.perform(
                get(ID_QUERY_RESOURCE + "20")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.*",contains("Resource not found")));
    }

    @Test
    void invalidParametersFromIdEndpointReturnBadRequest() throws Exception {
        // given
        storeManager.saveDocs(DataStoreManager.StoreType.TAXONOMY,createDocument(10L,false));

        // when
        ResultActions response = mockMvc.perform(
                get(ID_QUERY_RESOURCE + "invalid")
                        .param(FIELDS_PARAM,"invalid1, invalid2")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.*",
                        containsInAnyOrder("The taxonomy id value should be a number",
                                "Invalid fields parameter value 'invalid1'",
                                "Invalid fields parameter value 'invalid2'")));
    }



    @Test
    void canGetValidEntryFromIdEndpoint() throws Exception {
        // given
        storeManager.saveDocs(DataStoreManager.StoreType.TAXONOMY,createDocument(10L,false));

        // when
        ResultActions response = mockMvc.perform(
                get(ID_QUERY_RESOURCE+"10")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.taxonId",is(10)))
                .andExpect(jsonPath("$.scientificName",is("scientific10")));

        storeManager.cleanSolr(DataStoreManager.StoreType.TAXONOMY);
    }


    @Test
    void validSearchByTaxonId() throws Exception {
        // given
        storeManager.saveDocs(DataStoreManager.StoreType.TAXONOMY,createDocument(10L,true));

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_QUERY_RESOURCE)
                        .param(QUERY_PARAM,"tax_id:10")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.results.*.taxonId",contains(10)))
                .andExpect(jsonPath("$.results.*.scientificName",contains("scientific10")));

        storeManager.cleanSolr(DataStoreManager.StoreType.TAXONOMY);
    }


    @Test
    void validSearchByTaxonIdReturningFacets() throws Exception {
        // given
        storeManager.saveDocs(DataStoreManager.StoreType.TAXONOMY,createDocument(10L,true));
        storeManager.saveDocs(DataStoreManager.StoreType.TAXONOMY,createDocument(11L,false));
        storeManager.saveDocs(DataStoreManager.StoreType.TAXONOMY,createDocument(12L,true));
        storeManager.saveDocs(DataStoreManager.StoreType.TAXONOMY,createDocument(13L,false));

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_QUERY_RESOURCE)
                        .param(QUERY_PARAM,"scientific:*")
                        .param(FACETS_PARAM,"annotated,reviewed,complete,reference")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.results.*.taxonId",contains(10,11,12,13)))
                .andExpect(jsonPath("$.results.*.scientificName",hasItem("scientific10")));

        storeManager.cleanSolr(DataStoreManager.StoreType.TAXONOMY);
    }

    @Test
    void validDownloadEntries() throws Exception {
        // given
        IntStream.rangeClosed(1, 101)
                .forEach(i -> storeManager.saveDocs(DataStoreManager.StoreType.TAXONOMY,createDocument(i,false)));

        // when
        ResultActions response = mockMvc.perform(
                get(DOWNLOAD_QUERY_RESOURCE)
                        .param(QUERY_PARAM,"scientific:*")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,APPLICATION_JSON_VALUE))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.results.size()",is(101)));

        storeManager.cleanSolr(DataStoreManager.StoreType.TAXONOMY);
    }

    private TaxonomyDocument createDocument(long id,boolean facets){
        TaxonomyEntryBuilder entryBuilder = new TaxonomyEntryBuilder();
        TaxonomyEntry taxonomyEntry = entryBuilder.taxonId(id).scientificName("scientific"+id).build();

        TaxonomyDocument.TaxonomyDocumentBuilder builder = TaxonomyDocument.builder();
        return builder.id(String.valueOf(id))
                .taxId(id)
                .synonym("synonym"+id)
                .scientific("scientific"+id)
                .common("common"+id)
                .mnemonic("mnemonic"+id)
                .rank(TaxonomyRank.FAMILY.name())
                .lineage(Collections.singletonList(id -1))
                .strain(Collections.singletonList("strain"+id))
                .host(Collections.singletonList(id -2))
                .reviewed(facets)
                .reference(facets)
                .complete(facets)
                .annotated(facets)
                .taxonomyObj(getTaxonomyBinary(taxonomyEntry))
                .build();
    }

    private ByteBuffer getTaxonomyBinary(TaxonomyEntry entry) {
        try {
            return ByteBuffer.wrap(TaxonomyJsonConfig.getInstance().getFullObjectMapper().writeValueAsBytes(entry));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to parse TaxonomyEntry to binary json: ", e);
        }
    }
}