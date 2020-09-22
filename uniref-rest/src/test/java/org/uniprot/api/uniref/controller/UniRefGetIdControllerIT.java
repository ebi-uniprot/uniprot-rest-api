package org.uniprot.api.uniref.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.uniprot.api.uniref.controller.UniRefControllerITUtils.*;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
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
import org.uniprot.core.uniref.impl.RepresentativeMemberBuilder;
import org.uniprot.core.uniref.impl.UniRefEntryBuilder;
import org.uniprot.core.uniref.impl.UniRefEntryLightBuilder;
import org.uniprot.core.uniref.impl.UniRefMemberBuilder;
import org.uniprot.core.xml.jaxb.uniref.Entry;
import org.uniprot.core.xml.uniref.UniRefEntryConverter;
import org.uniprot.core.xml.uniref.UniRefEntryLightConverter;
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
        UniRefEntryConverter converter = new UniRefEntryConverter();
        UniRefEntryLightConverter unirefLightConverter = new UniRefEntryLightConverter();
        Entry entry = converter.toXml(unirefEntry);
        UniRefEntryLight entryLight = unirefLightConverter.fromXml(entry);
        getStoreManager().saveToStore(DataStoreManager.StoreType.UNIREF_LIGHT, entryLight);
        List<RepresentativeMember> members = createEntryMembers(unirefEntry);
        members.forEach(
                member -> {
                    getStoreManager().saveToStore(DataStoreManager.StoreType.UNIREF_MEMBER, member);
                });
    }

    @Test
    void getIdCompleteTrueReturnAllMembersWithoutPagination() throws Exception {
        // given
        UniRefEntry entry = createEntry(1, 28, UniRefType.UniRef50);
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
                .andExpect(jsonPath("$.name", is(NAME)))
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
        UniRefEntry entry = createEntry(1, 28, UniRefType.UniRef50);
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
        UniRefEntry entry = createEntry(1, 15, UniRefType.UniRef50);
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
        UniRefEntry entry = createEntry(1, 15, UniRefType.UniRef50);
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

    @Test
    void filterMemberTypes() throws Exception {
        // given
        UniRefEntry builder = getUniRefEntryForFilter();
        saveEntry(builder);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getIdRequestPath() + ID)
                                        .param("size", "10")
                                        .param("filter", "member_id_type:uniparc")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string("X-TotalRecords", "2"))
                .andExpect(header().string(HttpHeaders.LINK, nullValue()))
                .andExpect(jsonPath("$.id", is("UniRef50_P03901")))
                .andExpect(jsonPath("$.memberCount", is(6)))
                .andExpect(jsonPath("$.members.size()", is(2)))
                .andExpect(
                        jsonPath("$.members[*].memberId", contains("UP123456788", "UP123456789")))
                .andExpect(jsonPath("$.members[*].memberIdType", contains("UniParc", "UniParc")));
    }

    @Test
    void filterUniProtMemberTypes() throws Exception {
        // given
        UniRefEntry builder = getUniRefEntryForFilter();
        saveEntry(builder);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getIdRequestPath() + ID)
                                        .param("size", "10")
                                        .param(
                                                "filter",
                                                "uniprot_member_id_type:uniprotkb_unreviewed_trembl")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string("X-TotalRecords", "1"))
                .andExpect(header().string(HttpHeaders.LINK, nullValue()))
                .andExpect(jsonPath("$.id", is("UniRef50_P03901")))
                .andExpect(jsonPath("$.memberCount", is(6)))
                .andExpect(jsonPath("$.members.size()", is(1)))
                .andExpect(jsonPath("$.members[*].memberId", contains("P32106_HUMAN")))
                .andExpect(
                        jsonPath(
                                "$.members[*].memberIdType",
                                contains("UniProtKB Unreviewed (TrEMBL)")));
    }

    @Test
    void filterInvalidQueryField() throws Exception {
        // given
        UniRefEntry builder = getUniRefEntryForFilter();
        saveEntry(builder);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getIdRequestPath() + ID)
                                        .param("size", "10")
                                        .param("filter", "invalid:invalid")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string(HttpHeaders.LINK, nullValue()))
                .andExpect(jsonPath("$.url", not(emptyOrNullString())))
                .andExpect(jsonPath("$.messages.size()", is(1)))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains(
                                        "Invalid facet name 'invalid'. Expected value can be [member_id_type, uniprot_member_id_type].")));
    }

    @Test
    void filterInvalidQuerySyntax() throws Exception {
        // given
        UniRefEntry builder = getUniRefEntryForFilter();
        saveEntry(builder);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getIdRequestPath() + ID)
                                        .param("size", "10")
                                        .param("filter", "invalid:}invalid{")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string(HttpHeaders.LINK, nullValue()))
                .andExpect(jsonPath("$.url", not(emptyOrNullString())))
                .andExpect(jsonPath("$.messages.size()", is(1)))
                .andExpect(
                        jsonPath(
                                "$.messages.*", contains("query parameter has an invalid syntax")));
    }

    @Test
    void getFacetsSuccess() throws Exception {
        // given
        UniRefEntryLight entryLight =
                new UniRefEntryLightBuilder()
                        .id(ID)
                        .membersAdd(
                                "UP1234566787," + UniRefMemberIdType.UNIPARC.getMemberIdTypeId())
                        .membersAdd(
                                "UP1234566788," + UniRefMemberIdType.UNIPARC.getMemberIdTypeId())
                        .membersAdd(
                                "UP1234566789," + UniRefMemberIdType.UNIPARC.getMemberIdTypeId())
                        .membersAdd(
                                "P12344,"
                                        + UniRefMemberIdType.UNIPROTKB_SWISSPROT
                                                .getMemberIdTypeId())
                        .membersAdd(
                                "P12345,"
                                        + UniRefMemberIdType.UNIPROTKB_SWISSPROT
                                                .getMemberIdTypeId())
                        .membersAdd(
                                "P12346," + UniRefMemberIdType.UNIPROTKB_TREMBL.getMemberIdTypeId())
                        .membersAdd(
                                "P12347," + UniRefMemberIdType.UNIPROTKB_TREMBL.getMemberIdTypeId())
                        .membersAdd(
                                "P12348," + UniRefMemberIdType.UNIPROTKB_TREMBL.getMemberIdTypeId())
                        .membersAdd(
                                "P12349," + UniRefMemberIdType.UNIPROTKB_TREMBL.getMemberIdTypeId())
                        .build();
        getStoreManager().saveToStore(DataStoreManager.StoreType.UNIREF_LIGHT, entryLight);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get("/uniref/" + ID + "/facets")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.size()", is(2)))
                .andExpect(jsonPath("$.[0].label", is("Member Types")))
                .andExpect(jsonPath("$.[0].name", is("member_id_type")))
                .andExpect(jsonPath("$.[0].values.size()", is(2)))
                .andExpect(jsonPath("$.[0].values[*].value", hasItems("uniprotkb_id", "uniparc")))
                .andExpect(jsonPath("$.[0].values[*].count", hasItems(6, 3)))
                .andExpect(jsonPath("$.[1].label", is("UniProtKB Member Types")))
                .andExpect(jsonPath("$.[1].name", is("uniprot_member_id_type")))
                .andExpect(jsonPath("$.[1].values.size()", is(2)))
                .andExpect(
                        jsonPath(
                                "$.[1].values[*].value",
                                hasItems(
                                        "uniprotkb_unreviewed_trembl",
                                        "uniprotkb_reviewed_swissprot")))
                .andExpect(jsonPath("$.[1].values[*].count", hasItems(4, 2)));
    }

    @Test
    void getFacetsBadRequest() throws Exception {
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get("/uniref/INVALID/facets")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.url", not(emptyOrNullString())))
                .andExpect(jsonPath("$.messages.size()", is(1)))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains(
                                        "The 'id' value has invalid format. It should be a valid UniRef Cluster id")));
    }

    @Test
    void getFacetsNotFound() throws Exception {
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get("/uniref/UniRef50_P12345/facets")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.url", not(emptyOrNullString())))
                .andExpect(jsonPath("$.messages.size()", is(1)))
                .andExpect(jsonPath("$.messages.*", contains("Resource not found")));
    }

    @NotNull
    private UniRefEntry getUniRefEntryForFilter() {
        UniRefEntryBuilder builder = new UniRefEntryBuilder();
        builder.id(ID);
        builder.name("Cluster Name");
        builder.commonTaxonId(9606L);
        builder.entryType(UniRefType.UniRef50);

        RepresentativeMemberBuilder repBuilder =
                RepresentativeMemberBuilder.from(createReprestativeMember(1));
        repBuilder.uniparcId(null);
        repBuilder.memberId("CDC7_HUMAN");
        builder.representativeMember(repBuilder.build());

        builder.membersAdd(
                UniRefMemberBuilder.from(createMember(2))
                        .memberIdType(UniRefMemberIdType.UNIPROTKB)
                        .memberId("CDC7_HUMAN")
                        .uniparcId(null)
                        .build());
        builder.membersAdd(
                UniRefMemberBuilder.from(createMember(3))
                        .memberIdType(UniRefMemberIdType.UNIPROTKB)
                        .memberId("CDC8_HUMAN")
                        .uniparcId(null)
                        .build());

        builder.membersAdd(
                UniRefMemberBuilder.from(createMember(4))
                        .memberIdType(UniRefMemberIdType.UNIPARC)
                        .memberId("UP123456788")
                        .accessionsSet(new ArrayList<>())
                        .build());
        builder.membersAdd(
                UniRefMemberBuilder.from(createMember(5))
                        .memberIdType(UniRefMemberIdType.UNIPARC)
                        .memberId("UP123456789")
                        .accessionsSet(new ArrayList<>())
                        .build());

        builder.membersAdd(
                UniRefMemberBuilder.from(createMember(6))
                        .memberIdType(UniRefMemberIdType.UNIPROTKB_TREMBL)
                        .uniparcId(null)
                        .build());
        builder.memberCount(6);
        return builder.build();
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
