package org.uniprot.api.support.data.taxonomy.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.rest.controller.AbstractGetByIdWithTypeExtensionControllerIT;
import org.uniprot.api.rest.controller.param.ContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdParameter;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetIdContentTypeParamResolver;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetIdParameterResolver;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.service.RdfPrologs;
import org.uniprot.api.support.data.DataStoreTestConfig;
import org.uniprot.api.support.data.SupportDataRestApplication;
import org.uniprot.api.rest.respository.taxonomy.TaxonomyRepository;
import org.uniprot.core.json.parser.taxonomy.TaxonomyJsonConfig;
import org.uniprot.core.taxonomy.TaxonomyEntry;
import org.uniprot.core.taxonomy.TaxonomyInactiveReasonType;
import org.uniprot.core.taxonomy.impl.TaxonomyEntryBuilder;
import org.uniprot.core.taxonomy.impl.TaxonomyInactiveReasonBuilder;
import org.uniprot.core.uniprotkb.taxonomy.impl.TaxonomyBuilder;
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
class TaxonomyGetIdControllerIT extends AbstractGetByIdWithTypeExtensionControllerIT {

    @MockBean(name = "supportDataRdfRestTemplate")
    private RestTemplate restTemplate;

    private static final String TAX_ID = "9606";

    private static final String MERGED_TAX_ID = "100";

    private static final String DELETED_TAX_ID = "200";

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
        return "/taxonomy/{taxonId}";
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
                        .parent(
                                new TaxonomyBuilder()
                                        .taxonId(9000L)
                                        .scientificName("name9000")
                                        .commonName("common9000")
                                        .build())
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
                get(getIdRequestPath(), MERGED_TAX_ID).header(ACCEPT, MediaType.APPLICATION_JSON);

        ResultActions response = getMockMvc().perform(requestBuilder);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.SEE_OTHER.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        header().string(HttpHeaders.LOCATION, "/taxonomy/99?from=" + MERGED_TAX_ID))
                .andExpect(jsonPath("$.taxonId", is(100)))
                .andExpect(jsonPath("$.active", is(false)))
                .andExpect(jsonPath("$.inactiveReason.inactiveReasonType", is("MERGED")))
                .andExpect(jsonPath("$.inactiveReason.mergedTo", is(99)));
    }

    @Test
    void validDeletedEntryReturnSuccess() throws Exception {
        // given
        saveDeletedEntry();

        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getIdRequestPath(), DELETED_TAX_ID).header(ACCEPT, MediaType.APPLICATION_JSON);

        ResultActions response = getMockMvc().perform(requestBuilder);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.taxonId", is(Integer.parseInt(DELETED_TAX_ID))))
                .andExpect(jsonPath("$.active", is(false)))
                .andExpect(
                        jsonPath(
                                "$.inactiveReason.inactiveReasonType",
                                is(TaxonomyInactiveReasonType.DELETED.getName())));
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

    private void saveDeletedEntry() {
        long deletedTaxId = Long.parseLong(DELETED_TAX_ID);

        TaxonomyEntryBuilder entryBuilder = new TaxonomyEntryBuilder();
        TaxonomyEntry taxonomyEntry =
                entryBuilder
                        .taxonId(deletedTaxId)
                        .active(false)
                        .inactiveReason(
                                new TaxonomyInactiveReasonBuilder()
                                        .inactiveReasonType(TaxonomyInactiveReasonType.DELETED)
                                        .build())
                        .build();

        TaxonomyDocument document =
                TaxonomyDocument.builder()
                        .id(DELETED_TAX_ID)
                        .taxId(deletedTaxId)
                        .active(false)
                        .taxonomyObj(getTaxonomyBinary(taxonomyEntry))
                        .build();

        this.getStoreManager().saveDocs(DataStoreManager.StoreType.TAXONOMY, document);
    }

    private byte[] getTaxonomyBinary(TaxonomyEntry entry) {
        try {
            return TaxonomyJsonConfig.getInstance().getFullObjectMapper().writeValueAsBytes(entry);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to parse TaxonomyEntry to binary json: ", e);
        }
    }

    @Override
    protected RestTemplate getRestTemple() {
        return restTemplate;
    }

    @Override
    protected String getSearchAccession() {
        return TAX_ID;
    }

    @Override
    protected String getRdfProlog() {
        return RdfPrologs.TAXONOMY_PROLOG;
    }

    @Override
    protected String getIdRequestPathWithoutPathVariable() {
        return "/taxonomy/";
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
                    .resultMatcher(jsonPath("$.url", not(is(emptyOrNullString()))))
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
                    .resultMatcher(jsonPath("$.url", not(is(emptyOrNullString()))))
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
                    .resultMatcher(jsonPath("$.url", not(is(emptyOrNullString()))))
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
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.RDF_MEDIA_TYPE)
                                    .resultMatcher(
                                            content().contentType(UniProtMediaType.RDF_MEDIA_TYPE))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TURTLE_MEDIA_TYPE)
                                    .resultMatcher(
                                            content().contentType(UniProtMediaType.TURTLE_MEDIA_TYPE))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.N_TRIPLES_MEDIA_TYPE)
                                    .resultMatcher(
                                            content().contentType(UniProtMediaType.N_TRIPLES_MEDIA_TYPE))
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
                                    .resultMatcher(jsonPath("$.url", not(is(emptyOrNullString()))))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.messages.*",
                                                    contains(
                                                            "The taxonomy id value should be a number")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            is(
                                                                    "Error messages\nThe taxonomy id value should be a number")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            is(
                                                                    "Error messages\nThe taxonomy id value should be a number")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                                    .resultMatcher(
                                            content().contentType(UniProtMediaType.XLS_MEDIA_TYPE))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.RDF_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "The taxonomy id value should be a number")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TURTLE_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "The taxonomy id value should be a number")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.N_TRIPLES_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "The taxonomy id value should be a number")))
                                    .build())
                    .build();
        }
    }
}
