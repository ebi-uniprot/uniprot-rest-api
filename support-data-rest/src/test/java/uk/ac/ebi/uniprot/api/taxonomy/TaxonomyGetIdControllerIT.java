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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.ac.ebi.uniprot.api.DataStoreTestConfig;
import uk.ac.ebi.uniprot.api.rest.controller.AbstractGetByIdControllerIT;
import uk.ac.ebi.uniprot.api.rest.controller.param.ContentTypeParam;
import uk.ac.ebi.uniprot.api.rest.controller.param.GetIdContentTypeParam;
import uk.ac.ebi.uniprot.api.rest.controller.param.GetIdParameter;
import uk.ac.ebi.uniprot.api.rest.controller.param.resolver.AbstractGetIdContentTypeParamResolver;
import uk.ac.ebi.uniprot.api.rest.controller.param.resolver.AbstractGetIdParameterResolver;
import uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType;
import uk.ac.ebi.uniprot.api.support_data.SupportDataApplication;
import uk.ac.ebi.uniprot.domain.taxonomy.TaxonomyEntry;
import uk.ac.ebi.uniprot.domain.taxonomy.TaxonomyInactiveReasonType;
import uk.ac.ebi.uniprot.domain.taxonomy.builder.TaxonomyEntryBuilder;
import uk.ac.ebi.uniprot.domain.taxonomy.builder.TaxonomyInactiveReasonBuilder;
import uk.ac.ebi.uniprot.indexer.DataStoreManager;
import uk.ac.ebi.uniprot.json.parser.taxonomy.TaxonomyJsonConfig;
import uk.ac.ebi.uniprot.search.document.taxonomy.TaxonomyDocument;

import java.nio.ByteBuffer;
import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ContextConfiguration(classes= {DataStoreTestConfig.class, SupportDataApplication.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(TaxonomyController.class)
@ExtendWith(value = {SpringExtension.class, TaxonomyGetIdControllerIT.TaxonomyGetIdParameterResolver.class,
        TaxonomyGetIdControllerIT.TaxonomyGetIdContentTypeParamResolver.class})
public class TaxonomyGetIdControllerIT extends AbstractGetByIdControllerIT {

    private static final String TAX_ID = "9606";

    private static final String MERGED_TAX_ID = "100";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataStoreManager storeManager;

    @Override
    public MockMvc getMockMvc() {
        return mockMvc;
    }

    @Override
    public String getIdRequestPath() {
        return "/taxonomy/";
    }

    @Override
    public void saveEntry() {
        long taxId = Long.valueOf(TAX_ID);

        TaxonomyEntryBuilder entryBuilder = new TaxonomyEntryBuilder();
        TaxonomyEntry taxonomyEntry = entryBuilder.taxonId(taxId)
                .scientificName("scientific")
                .commonName("common")
                .mnemonic("mnemonic")
                .parentId(9000L)
                .links(Collections.singletonList("link"))
                .build();

        TaxonomyDocument document = TaxonomyDocument.builder()
                .id(TAX_ID)
                .taxId(taxId)
                .synonym("synonym")
                .scientific("scientific")
                .taxonomyObj(getTaxonomyBinary(taxonomyEntry))
                .build();

        storeManager.saveDocs(DataStoreManager.StoreType.TAXONOMY,document);
    }

    @Test
    void validMergedEntryReturnRedirectSeeOther() throws Exception {
        // given
        saveMergedEntry();

        // when
        MockHttpServletRequestBuilder requestBuilder = get(getIdRequestPath() + MERGED_TAX_ID)
                .header(ACCEPT, MediaType.APPLICATION_JSON);

        ResultActions response = getMockMvc().perform(requestBuilder);

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.SEE_OTHER.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(header().string(HttpHeaders.LOCATION, "/taxonomy/99"))
                .andExpect(jsonPath("$.taxonId", is(100)))
                .andExpect(jsonPath("$.active", is(false)))
                .andExpect(jsonPath("$.inactiveReason.inactiveReasonType", is("MERGED")))
                .andExpect(jsonPath("$.inactiveReason.mergedTo", is(99)));
    }

    private void saveMergedEntry() {
        long mergedTaxId = Long.valueOf(MERGED_TAX_ID);

        TaxonomyEntryBuilder entryBuilder = new TaxonomyEntryBuilder();
        TaxonomyEntry taxonomyEntry = entryBuilder.taxonId(mergedTaxId)
                .active(false)
                .inactiveReason(new TaxonomyInactiveReasonBuilder()
                        .inactiveReasonType(TaxonomyInactiveReasonType.MERGED)
                        .mergedTo(mergedTaxId - 1)
                        .build())
                .build();

        TaxonomyDocument document = TaxonomyDocument.builder()
                .id(MERGED_TAX_ID)
                .taxId(Long.valueOf(MERGED_TAX_ID))
                .active(false)
                .taxonomyObj(getTaxonomyBinary(taxonomyEntry))
                .build();

        storeManager.saveDocs(DataStoreManager.StoreType.TAXONOMY, document);
    }

    private ByteBuffer getTaxonomyBinary(TaxonomyEntry entry) {
        try {
            return ByteBuffer.wrap(TaxonomyJsonConfig.getInstance().getFullObjectMapper().writeValueAsBytes(entry));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to parse TaxonomyEntry to binary json: ", e);
        }
    }

    static class TaxonomyGetIdParameterResolver extends AbstractGetIdParameterResolver {

        @Override
        public GetIdParameter validIdParameter() {
            return GetIdParameter.builder().id(TAX_ID)
                    .resultMatcher(jsonPath("$.taxonId",is(9606)))
                    .resultMatcher(jsonPath("$.scientificName",is("scientific")))
                    .resultMatcher(jsonPath("$.commonName",is("common")))
                    .resultMatcher(jsonPath("$.mnemonic",is("mnemonic")))
                    .resultMatcher(jsonPath("$.links",contains("link")))
                    .build();
        }

        @Override
        public GetIdParameter invalidIdParameter() {
            return GetIdParameter.builder().id("INVALID")
                    .resultMatcher(jsonPath("$.url",not(isEmptyOrNullString())))
                    .resultMatcher(jsonPath("$.messages.*",contains("The taxonomy id value should be a number")))
                    .build();
        }

        @Override
        public GetIdParameter nonExistentIdParameter() {
            return GetIdParameter.builder().id("10")
                    .resultMatcher(jsonPath("$.url",not(isEmptyOrNullString())))
                    .resultMatcher(jsonPath("$.messages.*",contains("Resource not found")))
                    .build();
        }

        @Override
        public GetIdParameter withFilterFieldsParameter() {
            return GetIdParameter.builder().id(TAX_ID).fields("id,scientific_name")
                    .resultMatcher(jsonPath("$.taxonId",is(9606)))
                    .resultMatcher(jsonPath("$.scientificName",is("scientific")))
                    .resultMatcher(jsonPath("$.commonName").doesNotExist())
                    .resultMatcher(jsonPath("$.mnemonic").doesNotExist())
                    .resultMatcher(jsonPath("$.links").doesNotExist())
                    .build();
        }

        @Override
        public GetIdParameter withInvalidFilterParameter() {
            return GetIdParameter.builder().id("9606").fields("invalid")
                    .resultMatcher(jsonPath("$.url",not(isEmptyOrNullString())))
                    .resultMatcher(jsonPath("$.messages.*", contains("Invalid fields parameter value 'invalid'")))
                    .build();
        }
    }

    static class TaxonomyGetIdContentTypeParamResolver extends AbstractGetIdContentTypeParamResolver {

        @Override
        public GetIdContentTypeParam idSuccessContentTypesParam() {
            return GetIdContentTypeParam.builder()
                    .id("9606")
                    .contentTypeParam(ContentTypeParam.builder()
                            .contentType(MediaType.APPLICATION_JSON)
                            .resultMatcher(jsonPath("$.taxonId",is(9606)))
                            .resultMatcher(jsonPath("$.scientificName",is("scientific")))
                            .resultMatcher(jsonPath("$.commonName",is("common")))
                            .resultMatcher(jsonPath("$.mnemonic",is("mnemonic")))
                            .resultMatcher(jsonPath("$.links",contains("link")))
                            .build())
                    .contentTypeParam(ContentTypeParam.builder()
                            .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                            .resultMatcher(content().string(containsString("9606")))
                            .build())
                    .contentTypeParam(ContentTypeParam.builder()
                            .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                            .resultMatcher(content().string(containsString("Taxon\tMnemonic\tScientific name\tCommon name\tOther Names\tReviewed\tRank\tLineage\tParent\tVirus hosts")))
                            .resultMatcher(content().string(containsString("9606\tmnemonic\tscientific\tcommon\t\t\t\t\t9000")))
                            .build())
                    .contentTypeParam(ContentTypeParam.builder()
                            .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                            .resultMatcher(content().contentType(UniProtMediaType.XLS_MEDIA_TYPE))
                            .build())
                    .build();
        }

        @Override
        public GetIdContentTypeParam idBadRequestContentTypesParam() {
            return GetIdContentTypeParam.builder()
                    .id("INVALID")
                    .contentTypeParam(ContentTypeParam.builder()
                            .contentType(MediaType.APPLICATION_JSON)
                            .resultMatcher(jsonPath("$.url",not(isEmptyOrNullString())))
                            .resultMatcher(jsonPath("$.messages.*",contains("The taxonomy id value should be a number")))
                            .build())
                    .contentTypeParam(ContentTypeParam.builder()
                            .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                            .resultMatcher(content().string(isEmptyString()))
                            .build())
                    .contentTypeParam(ContentTypeParam.builder()
                            .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                            .resultMatcher(content().string(isEmptyString()))
                            .build())
                    .contentTypeParam(ContentTypeParam.builder()
                            .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                            .resultMatcher(content().string(isEmptyString()))
                            .build())
                    .build();
        }
    }
}
