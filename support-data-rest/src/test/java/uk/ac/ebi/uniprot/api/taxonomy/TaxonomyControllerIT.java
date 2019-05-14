package uk.ac.ebi.uniprot.api.taxonomy;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
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

import static org.hamcrest.Matchers.is;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes= {DataStoreTestConfig.class, SupportDataApplication.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(TaxonomyController.class)
class TaxonomyControllerIT {

    private static final String ID_QUERY_RESOURCE = "/taxonomy/";
    private static final String SEARCH_QUERY_RESOURCE = "/taxonomy/search";

    private static final String QUERY_PARAM = "query";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataStoreManager storeManager;

    @Test
    void validGetTaxonById() throws Exception {
        // given
        storeManager.saveDocs(DataStoreManager.StoreType.TAXONOMY,createDocument(10L));

        // when
        ResultActions response = mockMvc.perform(
                get(ID_QUERY_RESOURCE+"10")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.taxonId",is(10)))
                .andExpect(jsonPath("$.scientificName",is("scientific10")));

        storeManager.cleanSolr(DataStoreManager.StoreType.TAXONOMY);
    }


    @Test
    void validSearchByTaxonId() throws Exception {
        // given
        storeManager.saveDocs(DataStoreManager.StoreType.TAXONOMY,createDocument(10L));

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_QUERY_RESOURCE)
                        .param(QUERY_PARAM,"tax_id:10")
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.taxonId",is(10)))
                .andExpect(jsonPath("$.scientificName",is("scientific10")));

        storeManager.cleanSolr(DataStoreManager.StoreType.TAXONOMY);
    }

    @Test
    void search() {
    }



    private TaxonomyDocument createDocument(long id){
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
                .taxonomyObj(getTaxonomyBinary(entryBuilder.build()))
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