package uk.ac.ebi.uniprot.api.taxonomy;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.ebi.uniprot.api.DataStoreTestConfig;
import uk.ac.ebi.uniprot.api.rest.controller.AbstractBasicControllerIT;
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

/**
 * @author lgonzales
 */
@ContextConfiguration(classes= {DataStoreTestConfig.class, SupportDataApplication.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(TaxonomyController.class)
@ExtendWith(value = {SpringExtension.class, TaxonomyPathParameterResolver.class, TaxonomyContentTypeParamResolver.class,
                    TaxonomyQueryParamResolver.class})
class TaxonomyControllerCommonIT extends AbstractBasicControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataStoreManager storeManager;

    @Override
    public void saveEntry(SAVE_CONTEXT context) {
        switch (context){
            case ID_SUCCESS:
            case ID_NOT_FOUND:
            case SEARCH_NOT_FOUND:
                long taxId = Long.valueOf(TaxonomyPathParameterResolver.TAX_ID);
                storeManager.saveDocs(DataStoreManager.StoreType.TAXONOMY,createDocument(taxId,false));
                break;
            case SEARCH_SUCCESS:
                IntStream.rangeClosed(11, 15)
                        .forEach(i -> storeManager.saveDocs(DataStoreManager.StoreType.TAXONOMY,createDocument(i,i % 2 == 0)));
        }
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

    @Override
    public MockMvc getMockMvc() {
        return mockMvc;
    }

    @Override
    public String getIdRequestPath() {
        return "/taxonomy/";
    }

    @Override
    public String getSearchRequestPath() {
        return "/taxonomy/search";
    }
}
