package org.uniprot.api.support.data.taxonomy.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.ByteBuffer;
import java.util.*;

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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.rest.controller.AbstractGetByIdControllerIT;
import org.uniprot.api.rest.controller.param.ContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdParameter;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetIdContentTypeParamResolver;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetIdParameterResolver;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.support.data.DataStoreTestConfig;
import org.uniprot.api.support.data.SupportDataRestApplication;
import org.uniprot.api.support.data.taxonomy.repository.TaxonomyRepository;
import org.uniprot.core.json.parser.taxonomy.TaxonomyJsonConfig;
import org.uniprot.core.taxonomy.TaxonomyEntry;
import org.uniprot.core.taxonomy.TaxonomyInactiveReasonType;
import org.uniprot.core.taxonomy.impl.TaxonomyEntryBuilder;
import org.uniprot.core.taxonomy.impl.TaxonomyInactiveReasonBuilder;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.taxonomy.TaxonomyDocument;

import com.fasterxml.jackson.core.JsonProcessingException;

@ContextConfiguration(classes = {DataStoreTestConfig.class, SupportDataRestApplication.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(TaxonomyController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
            TaxonomyGetIdControllerIT.TaxonomyGetIdParameterResolver.class,
            TaxonomyGetIdControllerIT.TaxonomyGetIdContentTypeParamResolver.class
        })
public class TaxonomyGetIdControllerIT extends AbstractGetByIdControllerIT {

    private static final String TAX_ID = "9606";

    private static final String MERGED_TAX_ID = "100";

    @Autowired private TaxonomyRepository repository;

    @Override
    protected DataStoreManager.StoreType getStoreType() {
        return DataStoreManager.StoreType.TAXONOMY;
    }

    @Override
    protected SolrCollection getSolrCollection() {
        return SolrCollection.taxonomy;
    }

    @Override
    protected SolrQueryRepository getRepository() {
        return repository;
    }

    @Override
    protected String getIdRequestPath() {
        return "/taxonomy/";
    }

    @Override
    protected void saveEntry() {
        long taxId = Long.parseLong(TAX_ID);

        TaxonomyEntryBuilder entryBuilder = new TaxonomyEntryBuilder();
        TaxonomyEntry taxonomyEntry =
                entryBuilder
                        .taxonId(taxId)
                        .scientificName("scientific")
                        .commonName("common")
                        .mnemonic("mnemonic")
                        .parentId(9000L)
                        .linksSet(Collections.singletonList("link"))
                        .build();

        TaxonomyDocument document =
                TaxonomyDocument.builder()
                        .id(TAX_ID)
                        .taxId(taxId)
                        .synonym("synonym")
                        .scientific("scientific")
                        .taxonomyObj(getTaxonomyBinary(taxonomyEntry))
                        .build();

        this.getStoreManager().saveDocs(DataStoreManager.StoreType.TAXONOMY, document);
    }

    @Test
    void validMergedEntryReturnRedirectSeeOther() throws Exception {
        // given
        saveMergedEntry();

        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getIdRequestPath() + MERGED_TAX_ID).header(ACCEPT, MediaType.APPLICATION_JSON);

        ResultActions response = getMockMvc().perform(requestBuilder);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.SEE_OTHER.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(header().string(HttpHeaders.LOCATION, "/taxonomy/99"))
                .andExpect(jsonPath("$.taxonId", is(100)))
                .andExpect(jsonPath("$.active", is(false)))
                .andExpect(jsonPath("$.inactiveReason.inactiveReasonType", is("MERGED")))
                .andExpect(jsonPath("$.inactiveReason.mergedTo", is(99)));
    }

    private void saveMergedEntry() {
        long mergedTaxId = Long.parseLong(MERGED_TAX_ID);

        TaxonomyEntryBuilder entryBuilder = new TaxonomyEntryBuilder();
        TaxonomyEntry taxonomyEntry =
                entryBuilder
                        .taxonId(mergedTaxId)
                        .active(false)
                        .inactiveReason(
                                new TaxonomyInactiveReasonBuilder()
                                        .inactiveReasonType(TaxonomyInactiveReasonType.MERGED)
                                        .mergedTo(mergedTaxId - 1)
                                        .build())
                        .build();

        TaxonomyDocument document =
                TaxonomyDocument.builder()
                        .id(MERGED_TAX_ID)
                        .taxId(Long.valueOf(MERGED_TAX_ID))
                        .active(false)
                        .taxonomyObj(getTaxonomyBinary(taxonomyEntry))
                        .build();

        this.getStoreManager().saveDocs(DataStoreManager.StoreType.TAXONOMY, document);
    }

    private ByteBuffer getTaxonomyBinary(TaxonomyEntry entry) {
        try {
            return ByteBuffer.wrap(
                    TaxonomyJsonConfig.getInstance()
                            .getFullObjectMapper()
                            .writeValueAsBytes(entry));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to parse TaxonomyEntry to binary json: ", e);
        }
    }

    static class TaxonomyGetIdParameterResolver extends AbstractGetIdParameterResolver {

        @Override
        public GetIdParameter validIdParameter() {
            return GetIdParameter.builder()
                    .id(TAX_ID)
                    .resultMatcher(jsonPath("$.taxonId", is(9606)))
                    .resultMatcher(jsonPath("$.scientificName", is("scientific")))
                    .resultMatcher(jsonPath("$.commonName", is("common")))
                    .resultMatcher(jsonPath("$.mnemonic", is("mnemonic")))
                    .resultMatcher(jsonPath("$.links", contains("link")))
                    .build();
        }

        @Override
        public GetIdParameter invalidIdParameter() {
            return GetIdParameter.builder()
                    .id("INVALID")
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    contains("The taxonomy id value should be a number")))
                    .build();
        }

        @Override
        public GetIdParameter nonExistentIdParameter() {
            return GetIdParameter.builder()
                    .id("10")
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(jsonPath("$.messages.*", contains("Resource not found")))
                    .build();
        }

        @Override
        public GetIdParameter withFilterFieldsParameter() {
            return GetIdParameter.builder()
                    .id(TAX_ID)
                    .fields("id,scientific_name")
                    .resultMatcher(jsonPath("$.taxonId", is(9606)))
                    .resultMatcher(jsonPath("$.scientificName", is("scientific")))
                    .resultMatcher(jsonPath("$.commonName").doesNotExist())
                    .resultMatcher(jsonPath("$.mnemonic").doesNotExist())
                    .resultMatcher(jsonPath("$.links").doesNotExist())
                    .build();
        }

        @Override
        public GetIdParameter withInvalidFilterParameter() {
            return GetIdParameter.builder()
                    .id("9606")
                    .fields("invalid")
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    contains("Invalid fields parameter value 'invalid'")))
                    .build();
        }
    }

    static class TaxonomyGetIdContentTypeParamResolver
            extends AbstractGetIdContentTypeParamResolver {

        @Override
        public GetIdContentTypeParam idSuccessContentTypesParam() {
            return GetIdContentTypeParam.builder()
                    .id("9606")
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(jsonPath("$.taxonId", is(9606)))
                                    .resultMatcher(jsonPath("$.scientificName", is("scientific")))
                                    .resultMatcher(jsonPath("$.commonName", is("common")))
                                    .resultMatcher(jsonPath("$.mnemonic", is("mnemonic")))
                                    .resultMatcher(jsonPath("$.links", contains("link")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(content().string(containsString("9606")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "Taxon Id\tMnemonic\tScientific name\tCommon name\tOther Names\tReviewed\tRank\tLineage\tParent\tVirus hosts")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "9606\tmnemonic\tscientific\tcommon\t\t\t\t\t9000")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                                    .resultMatcher(
                                            content().contentType(UniProtMediaType.XLS_MEDIA_TYPE))
                                    .build())
                    .build();
        }

        @Override
        public GetIdContentTypeParam idBadRequestContentTypesParam() {
            return GetIdContentTypeParam.builder()
                    .id("INVALID")
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.messages.*",
                                                    contains(
                                                            "The taxonomy id value should be a number")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(content().string(isEmptyString()))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(content().string(isEmptyString()))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                                    .resultMatcher(content().string(isEmptyString()))
                                    .build())
                    .build();
        }
    }
}
