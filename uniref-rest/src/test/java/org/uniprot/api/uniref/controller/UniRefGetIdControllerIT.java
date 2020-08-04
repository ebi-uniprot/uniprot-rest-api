package org.uniprot.api.uniref.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.uniprot.api.uniref.controller.UniRefControllerITUtils.*;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
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
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.rest.controller.AbstractGetByIdControllerIT;
import org.uniprot.api.rest.controller.param.ContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdParameter;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetIdContentTypeParamResolver;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetIdParameterResolver;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.api.uniref.UniRefRestApplication;
import org.uniprot.api.uniref.repository.DataStoreTestConfig;
import org.uniprot.api.uniref.repository.UniRefQueryRepository;
import org.uniprot.api.uniref.repository.store.UniRefLightStoreClient;
import org.uniprot.api.uniref.repository.store.UniRefMemberStoreClient;
import org.uniprot.core.uniref.*;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.indexer.uniref.UniRefDocumentConverter;
import org.uniprot.store.search.SolrCollection;

@ContextConfiguration(
        classes = {
            DataStoreTestConfig.class,
            UniRefRestApplication.class,
            ErrorHandlerConfig.class
        })
@ActiveProfiles(profiles = "offline")
@WebMvcTest(UniRefEntryController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
            UniRefGetIdControllerIT.UniRefGetIdParameterResolver.class,
            UniRefGetIdControllerIT.UniRefGetIdContentTypeParamResolver.class
        })
class UniRefGetIdControllerIT extends AbstractGetByIdControllerIT {
    private static final String ID = "UniRef50_P03901";
    private static final String NAME = "Cluster: MoeK5 01";

    @Autowired private UniRefQueryRepository repository;

    @Autowired private UniRefMemberStoreClient memberStoreClient;

    @Autowired private UniRefLightStoreClient lightStoreClient;

    @Override
    protected DataStoreManager.StoreType getStoreType() {
        return DataStoreManager.StoreType.UNIREF_LIGHT;
    }

    @Override
    protected SolrCollection getSolrCollection() {
        return SolrCollection.uniref;
    }

    @Override
    protected SolrQueryRepository getRepository() {
        return repository;
    }

    @Override
    protected String getIdRequestPath() {
        return "/uniref/";
    }

    @BeforeAll
    void initDataStore() {
        getStoreManager().addStore(DataStoreManager.StoreType.UNIREF_LIGHT, lightStoreClient);
        getStoreManager().addStore(DataStoreManager.StoreType.UNIREF_MEMBER, memberStoreClient);
        getStoreManager()
                .addDocConverter(
                        DataStoreManager.StoreType.UNIREF_LIGHT,
                        new UniRefDocumentConverter(TaxonomyRepoMocker.getTaxonomyRepo()));
    }

    @AfterEach
    void cleanStoreClient() {
        lightStoreClient.truncate();
        memberStoreClient.truncate();
    }

    @Override
    protected void saveEntry() {
        UniRefEntry unirefEntry = createEntry(1, UniRefType.UniRef50);
        saveEntry(unirefEntry);
    }

    private void saveEntry(UniRefEntry unirefEntry) {
        getStoreManager()
                .saveToStore(
                        DataStoreManager.StoreType.UNIREF_LIGHT, createEntryLight(unirefEntry));
        List<RepresentativeMember> members = createEntryMembers(unirefEntry);
        members.forEach(
                member -> {
                    getStoreManager().saveToStore(DataStoreManager.StoreType.UNIREF_MEMBER, member);
                });
    }

    @Test
    void getIdCompleteTrueReturnAllMembersWithoutPagination() throws Exception {
        // given
        UniRefEntry entry = UniRefControllerITUtils.createEntry(1, 28, UniRefType.UniRef50);
        saveEntry(entry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getIdRequestPath() + ID)
                                        .param("complete", "true")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string("X-TotalRecords", nullValue()))
                .andExpect(header().string(HttpHeaders.LINK, nullValue()))
                .andExpect(jsonPath("$.id", is(ID)))
                .andExpect(jsonPath("$.name", is("Cluster: MoeK5 01")))
                .andExpect(jsonPath("$.memberCount", is(28)))
                .andExpect(jsonPath("$.updated", is("2019-08-27")))
                .andExpect(jsonPath("$.entryType", is("UniRef50")))
                .andExpect(jsonPath("$.commonTaxonId", is(9606)))
                .andExpect(jsonPath("$.commonTaxon", is("Homo sapiens")))
                .andExpect(jsonPath("$.goTerms.size()", is(3)))
                .andExpect(jsonPath("$.representativeMember.memberIdType", is("UniProtKB ID")))
                .andExpect(jsonPath("$.representativeMember.memberId", is("P12301_HUMAN")))
                .andExpect(
                        jsonPath(
                                "$.representativeMember.organismName",
                                is("Homo sapiens (Representative)")))
                .andExpect(jsonPath("$.representativeMember.organismTaxId", is(9600)))
                .andExpect(jsonPath("$.representativeMember.sequenceLength", is(312)))
                .andExpect(jsonPath("$.representativeMember.proteinName", is("some protein name")))
                .andExpect(jsonPath("$.representativeMember.accessions[*]", contains("P12301")))
                .andExpect(jsonPath("$.representativeMember.uniref50Id").doesNotExist())
                .andExpect(jsonPath("$.representativeMember.uniref90Id", is("UniRef90_P03943")))
                .andExpect(jsonPath("$.representativeMember.uniref100Id", is("UniRef100_P03923")))
                .andExpect(jsonPath("$.representativeMember.uniparcId", is("UPI0000083A01")))
                .andExpect(jsonPath("$.representativeMember.seed", is(true)))
                .andExpect(jsonPath("$.representativeMember.sequence").exists())
                .andExpect(jsonPath("$.members[0].memberIdType", is("UniProtKB ID")))
                .andExpect(jsonPath("$.members[0].memberId", is("P32101_HUMAN")))
                .andExpect(jsonPath("$.members[0].organismName", is("Homo sapiens 1")))
                .andExpect(jsonPath("$.members[0].organismTaxId", is(9607)))
                .andExpect(jsonPath("$.members[0].sequenceLength", is(312)))
                .andExpect(jsonPath("$.members[0].proteinName", is("some protein name")))
                .andExpect(jsonPath("$.members[0].accessions[*]", contains("P32101")))
                .andExpect(jsonPath("$.members[0].uniref50Id").doesNotExist())
                .andExpect(jsonPath("$.members[0].uniref90Id", is("UniRef90_P03943")))
                .andExpect(jsonPath("$.members[0].uniref100Id", is("UniRef100_P03923")))
                .andExpect(jsonPath("$.members[0].uniparcId", is("UPI0000083A01")))
                .andExpect(jsonPath("$.members[0].seed").doesNotExist())
                .andExpect(jsonPath("$.members[0].sequence").doesNotExist())
                .andExpect(jsonPath("$.members.size()", is(27)));
    }

    @Test
    void getIdByDefaultReturnTwentyFiveMembersWithPagination() throws Exception {
        // given
        UniRefEntry entry = UniRefControllerITUtils.createEntry(1, 28, UniRefType.UniRef50);
        saveEntry(entry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getIdRequestPath() + ID)
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string("X-TotalRecords", "28"))
                .andExpect(header().string(HttpHeaders.LINK, notNullValue()))
                .andExpect(header().string(HttpHeaders.LINK, containsString("size=25")))
                .andExpect(header().string(HttpHeaders.LINK, containsString("cursor=3v5g94y9lqs")))
                .andExpect(jsonPath("$.id", is(ID)))
                .andExpect(jsonPath("$.representativeMember.memberId", is("P12301_HUMAN")))
                .andExpect(jsonPath("$.members.size()", is(24)))
                .andExpect(jsonPath("$.memberCount", is(28)));
    }

    @Test
    void getFirstPageMembers() throws Exception {
        // given
        UniRefEntry entry = UniRefControllerITUtils.createEntry(1, 15, UniRefType.UniRef50);
        saveEntry(entry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getIdRequestPath() + ID)
                                        .param("size", "10")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string("X-TotalRecords", "15"))
                .andExpect(header().string(HttpHeaders.LINK, notNullValue()))
                .andExpect(header().string(HttpHeaders.LINK, containsString("size=10")))
                .andExpect(header().string(HttpHeaders.LINK, containsString("cursor=3sbq7rwffis")))
                .andExpect(jsonPath("$.id", is(ID)))
                .andExpect(jsonPath("$.representativeMember.memberId", is("P12301_HUMAN")))
                .andExpect(jsonPath("$.members.size()", is(9)))
                .andExpect(
                        jsonPath(
                                "$.members[*].memberId",
                                contains(
                                        "P32101_HUMAN",
                                        "P32102_HUMAN",
                                        "P32103_HUMAN",
                                        "P32104_HUMAN",
                                        "P32105_HUMAN",
                                        "P32106_HUMAN",
                                        "P32107_HUMAN",
                                        "P32108_HUMAN",
                                        "P32109_HUMAN")))
                .andExpect(jsonPath("$.memberCount", is(15)));
    }

    @Test
    void getLastPageMembers() throws Exception {
        // given
        UniRefEntry entry = UniRefControllerITUtils.createEntry(1, 15, UniRefType.UniRef50);
        saveEntry(entry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getIdRequestPath() + ID)
                                        .param("size", "10")
                                        .param("cursor", "3sbq7rwffis")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string("X-TotalRecords", "15"))
                .andExpect(header().string(HttpHeaders.LINK, nullValue()))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.representativeMember").doesNotExist())
                .andExpect(jsonPath("$.memberCount").doesNotExist())
                .andExpect(jsonPath("$.members.size()", is(5)))
                .andExpect(
                        jsonPath(
                                "$.members[*].memberId",
                                contains(
                                        "P32110_HUMAN",
                                        "P32111_HUMAN",
                                        "P32112_HUMAN",
                                        "P32113_HUMAN",
                                        "P32114_HUMAN")));
    }

    static class UniRefGetIdParameterResolver extends AbstractGetIdParameterResolver {

        @Override
        public GetIdParameter validIdParameter() {
            return GetIdParameter.builder().id(ID).resultMatcher(jsonPath("$.id", is(ID))).build();
        }

        @Override
        public GetIdParameter invalidIdParameter() {
            return GetIdParameter.builder()
                    .id("INVALID")
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    contains(
                                            "The 'id' value has invalid format. It should be a valid UniRef Cluster id")))
                    .build();
        }

        @Override
        public GetIdParameter nonExistentIdParameter() {
            return GetIdParameter.builder()
                    .id("UniRef50_P03925")
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(jsonPath("$.messages.*", contains("Resource not found")))
                    .build();
        }

        @Override
        public GetIdParameter withFilterFieldsParameter() {
            return GetIdParameter.builder()
                    .id(ID)
                    .fields("name,count")
                    .resultMatcher(jsonPath("$.id", is(ID)))
                    .build();
        }

        @Override
        public GetIdParameter withInvalidFilterParameter() {
            return GetIdParameter.builder()
                    .id(ID)
                    .fields("invalid")
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    contains("Invalid fields parameter value 'invalid'")))
                    .build();
        }
    }

    static class UniRefGetIdContentTypeParamResolver extends AbstractGetIdContentTypeParamResolver {

        @Override
        public GetIdContentTypeParam idSuccessContentTypesParam() {
            return GetIdContentTypeParam.builder()
                    .id(ID)
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(jsonPath("$.id", is(ID)))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_XML)
                                    .resultMatcher(content().string(containsString(ID)))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(content().string(containsString(ID)))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.FASTA_MEDIA_TYPE)
                                    .resultMatcher(content().string(containsString(ID)))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "Cluster ID\tCluster Name\tCommon taxon\tSize\tDate of creation")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "UniRef50_P03901\tCluster: MoeK5 01\tHomo sapiens\t2\t2019-08-27")))
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
                                                            "The 'id' value has invalid format. It should be a valid UniRef Cluster id")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_XML)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "The 'id' value has invalid format. It should be a valid UniRef Cluster id")))
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
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.FASTA_MEDIA_TYPE)
                                    .resultMatcher(content().string(isEmptyString()))
                                    .build())
                    .build();
        }
    }
}
