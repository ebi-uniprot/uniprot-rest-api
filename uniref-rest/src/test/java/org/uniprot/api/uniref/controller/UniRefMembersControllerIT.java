package org.uniprot.api.uniref.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.LINK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.uniprot.api.rest.output.header.HttpCommonHeaderConfig.*;
import static org.uniprot.store.indexer.uniref.mockers.UniRefEntryMocker.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.validation.constraints.NotNull;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.api.uniref.UniRefRestApplication;
import org.uniprot.api.uniref.repository.DataStoreTestConfig;
import org.uniprot.api.uniref.repository.UniRefQueryRepository;
import org.uniprot.api.uniref.repository.store.UniRefEntryFacetConfig;
import org.uniprot.api.uniref.repository.store.UniRefLightStoreClient;
import org.uniprot.api.uniref.repository.store.UniRefMemberStoreClient;
import org.uniprot.core.uniprotkb.taxonomy.impl.OrganismBuilder;
import org.uniprot.core.uniref.RepresentativeMember;
import org.uniprot.core.uniref.UniRefEntry;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.core.uniref.UniRefMemberIdType;
import org.uniprot.core.uniref.UniRefType;
import org.uniprot.core.uniref.impl.RepresentativeMemberBuilder;
import org.uniprot.core.uniref.impl.UniRefEntryBuilder;
import org.uniprot.core.uniref.impl.UniRefMemberBuilder;
import org.uniprot.core.xml.jaxb.uniref.Entry;
import org.uniprot.core.xml.uniref.UniRefEntryConverter;
import org.uniprot.core.xml.uniref.UniRefEntryLightConverter;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.indexer.uniref.UniRefDocumentConverter;

/**
 * @author lgonzales
 * @since 08/01/2021
 */
@Slf4j
@ContextConfiguration(
        classes = {
            DataStoreTestConfig.class,
            UniRefRestApplication.class,
            ErrorHandlerConfig.class
        })
@ActiveProfiles(profiles = "offline")
@WebMvcTest(UniRefMemberController.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension.class)
class UniRefMembersControllerIT {

    private static final String ID_FILTER = "UniRef50_P00001";
    private static final String ID_50 = "UniRef50_P03901";
    private static final String ID_90 = "UniRef90_P03901";
    private static final String ID_100 = "UniRef100_P03901";
    private static final String NAME = "Cluster: MoeK5 01";
    private static final String MEMBER_PREFIX_PATH = "/uniref/";
    private static final String MEMBER_SUFIX_PATH = "/members";

    @Autowired private UniRefQueryRepository repository;

    @Autowired private UniRefMemberStoreClient memberStoreClient;

    @Autowired private UniRefLightStoreClient lightStoreClient;

    @Autowired private MockMvc mockMvc;
    @Autowired private UniRefEntryFacetConfig uniRefEntryFacetConfig;

    @RegisterExtension static DataStoreManager storeManager = new DataStoreManager();

    @BeforeAll
    void initDataStore() {
        storeManager.addStore(DataStoreManager.StoreType.UNIREF_LIGHT, lightStoreClient);
        storeManager.addStore(DataStoreManager.StoreType.UNIREF_MEMBER, memberStoreClient);
        storeManager.addDocConverter(
                DataStoreManager.StoreType.UNIREF_LIGHT,
                new UniRefDocumentConverter(TaxonomyRepoMocker.getTaxonomyRepo()));

        UniRefEntry entry = createEntry(1, 28, UniRefType.UniRef50);
        saveEntry(entry);

        entry = createEntry(1, 15, UniRefType.UniRef90);
        saveEntry(entry);

        entry = createEntry(1, UniRefType.UniRef100);
        saveEntry(entry);

        entry = getUniRefEntryForFilter();
        saveEntry(entry);
    }

    @AfterAll
    void cleanStoreClient() {
        lightStoreClient.truncate();
        memberStoreClient.truncate();
    }

    private void saveEntry(UniRefEntry unirefEntry) {
        UniRefEntryConverter converter = new UniRefEntryConverter();
        UniRefEntryLightConverter unirefLightConverter = new UniRefEntryLightConverter();
        Entry entry = converter.toXml(unirefEntry);
        UniRefEntryLight entryLight = unirefLightConverter.fromXml(entry);
        log.info(
                "Saving Entry: {} "
                        + "with Entry members count {} and "
                        + "EntryLight members count {}.",
                unirefEntry.getId().getValue(),
                unirefEntry.getMembers().size(),
                entryLight.getMembers().size());
        storeManager.saveToStore(DataStoreManager.StoreType.UNIREF_LIGHT, entryLight);
        List<RepresentativeMember> members = createEntryMembers(unirefEntry);
        members.forEach(
                member -> {
                    storeManager.saveToStore(DataStoreManager.StoreType.UNIREF_MEMBER, member);
                });
    }

    @Test
    void getMembersSuccessDefaultPage25AndFirstMemberIsRepresentative() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(MEMBER_PREFIX_PATH + ID_50 + MEMBER_SUFIX_PATH)
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string(X_TOTAL_RECORDS, is("28")))
                .andExpect(header().string(LINK, notNullValue()))
                .andExpect(header().string(LINK, containsString("size=25")))
                .andExpect(header().string(LINK, containsString("cursor=3v5g94y9lqs")))
                .andExpect(jsonPath("$.results.size()", is(25)))
                // result [0] is a representative with sequence
                .andExpect(
                        jsonPath("$.results[0].memberIdType", is("UniProtKB Unreviewed (TrEMBL)")))
                .andExpect(jsonPath("$.results[0].memberId", is("P12301_HUMAN")))
                .andExpect(
                        jsonPath("$.results[0].organismName", is("Homo sapiens (Representative)")))
                .andExpect(jsonPath("$.results[0].organismTaxId", is(9600)))
                .andExpect(jsonPath("$.results[0].sequenceLength", is(312)))
                .andExpect(jsonPath("$.results[0].proteinName", is("some protein name")))
                .andExpect(jsonPath("$.results[0].accessions[0]", is("P12301")))
                .andExpect(jsonPath("$.results[0].uniref50Id").doesNotExist())
                .andExpect(jsonPath("$.results[0].uniref90Id", is("UniRef90_P03943")))
                .andExpect(jsonPath("$.results[0].uniref100Id", is("UniRef100_P03923")))
                .andExpect(jsonPath("$.results[0].uniparcId", is("UPI0000083A01")))
                .andExpect(jsonPath("$.results[0].seed", is(true)))
                .andExpect(jsonPath("$.results[0].sequence").exists())
                .andExpect(jsonPath("$.results[0].sequence.length", is(66)))
                .andExpect(
                        jsonPath(
                                "$.results[0].sequence.md5",
                                is("A1AA1787022479BC981C0B4422684719")))
                // result [0] is a member without sequence
                .andExpect(jsonPath("$.results[1].memberIdType", is("UniProtKB ID")))
                .andExpect(jsonPath("$.results[1].memberId", is("P32101_HUMAN")))
                .andExpect(jsonPath("$.results[1].organismName", is("Homo sapiens 1")))
                .andExpect(jsonPath("$.results[1].organismTaxId", is(9607)))
                .andExpect(jsonPath("$.results[1].sequenceLength", is(312)))
                .andExpect(jsonPath("$.results[1].proteinName", is("some protein name")))
                .andExpect(jsonPath("$.results[1].accessions[0]", is("P32101")))
                .andExpect(jsonPath("$.results[1].uniref50Id").doesNotExist())
                .andExpect(jsonPath("$.results[1].uniref90Id", is("UniRef90_P03943")))
                .andExpect(jsonPath("$.results[1].uniref100Id", is("UniRef100_P03923")))
                .andExpect(jsonPath("$.results[1].uniparcId", is("UPI0000083A01")))
                .andExpect(jsonPath("$.results[1].seed").doesNotExist())
                .andExpect(jsonPath("$.results[1].sequence").doesNotExist());
    }

    @Test
    void getFirstPageMembers() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(MEMBER_PREFIX_PATH + ID_90 + MEMBER_SUFIX_PATH)
                                .param("size", "10")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string(X_TOTAL_RECORDS, "15"))
                .andExpect(header().string(LINK, notNullValue()))
                .andExpect(header().string(LINK, containsString("size=10")))
                .andExpect(header().string(LINK, containsString("cursor=3sbq7rwffis")))
                .andExpect(jsonPath("$.results.size()", is(10)))
                .andExpect(
                        jsonPath(
                                "$.results[*].memberId",
                                contains(
                                        "P12301_HUMAN",
                                        "P32101_HUMAN",
                                        "P32102_HUMAN",
                                        "P32103_HUMAN",
                                        "P32104_HUMAN",
                                        "P32105_HUMAN",
                                        "P32106_HUMAN",
                                        "P32107_HUMAN",
                                        "P32108_HUMAN",
                                        "P32109_HUMAN")));
    }

    @Test
    void getLastPageMembers() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(MEMBER_PREFIX_PATH + ID_90 + MEMBER_SUFIX_PATH)
                                .param("size", "10")
                                .param("cursor", "3sbq7rwffis")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string(X_TOTAL_RECORDS, "15"))
                .andExpect(header().string(LINK, nullValue()))
                .andExpect(jsonPath("$.results.size()", is(5)))
                .andExpect(
                        jsonPath(
                                "$.results[*].memberId",
                                contains(
                                        "P32110_HUMAN",
                                        "P32111_HUMAN",
                                        "P32112_HUMAN",
                                        "P32113_HUMAN",
                                        "P32114_HUMAN")));
    }

    @Test
    void facetFilterMemberTypes() throws Exception {
        // given
        UniRefEntry builder = getUniRefEntryForFilter();
        saveEntry(builder);

        // when
        ResultActions response =
                mockMvc.perform(
                        get(MEMBER_PREFIX_PATH + ID_FILTER + MEMBER_SUFIX_PATH)
                                .param("size", "10")
                                .param("facetFilter", "member_id_type:uniparc")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string(X_TOTAL_RECORDS, "2"))
                .andExpect(header().string(LINK, nullValue()))
                .andExpect(jsonPath("$.results.size()", is(2)))
                .andExpect(
                        jsonPath("$.results[*].memberId", contains("UP123456788", "UP123456789")))
                .andExpect(jsonPath("$.results[*].memberIdType", contains("UniParc", "UniParc")));
    }

    @Test
    void facetFilterUniProtMemberTypes() throws Exception {
        // given
        UniRefEntry builder = getUniRefEntryForFilter();
        saveEntry(builder);

        // when
        ResultActions response =
                mockMvc.perform(
                        get(MEMBER_PREFIX_PATH + ID_FILTER + MEMBER_SUFIX_PATH)
                                .param("size", "10")
                                .param(
                                        "facetFilter",
                                        "uniprot_member_id_type:uniprotkb_unreviewed_trembl")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string(X_TOTAL_RECORDS, "1"))
                .andExpect(header().string(LINK, nullValue()))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.results[*].memberId", contains("P32180_HUMAN")))
                .andExpect(
                        jsonPath(
                                "$.results[*].memberIdType",
                                contains("UniProtKB Unreviewed (TrEMBL)")));
    }

    @Test
    void facetFilterInvalidQueryField() throws Exception {
        // given
        UniRefEntry builder = getUniRefEntryForFilter();
        saveEntry(builder);

        // when
        ResultActions response =
                mockMvc.perform(
                        get(MEMBER_PREFIX_PATH + ID_50 + MEMBER_SUFIX_PATH)
                                .param("size", "10")
                                .param("facetFilter", "invalid:invalid")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string(LINK, nullValue()))
                .andExpect(jsonPath("$.url", not(emptyOrNullString())))
                .andExpect(jsonPath("$.messages.size()", is(1)))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains(
                                        "Invalid facet name 'invalid'. Expected value can be [member_id_type, uniprot_member_id_type].")));
    }

    @Test
    void facetFilterInvalidQuerySyntax() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(MEMBER_PREFIX_PATH + ID_50 + MEMBER_SUFIX_PATH)
                                .param("size", "10")
                                .param("facetFilter", "invalid:}invalid{")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string(LINK, nullValue()))
                .andExpect(jsonPath("$.url", not(emptyOrNullString())))
                .andExpect(jsonPath("$.messages.size()", is(1)))
                .andExpect(
                        jsonPath(
                                "$.messages.*", contains("query parameter has an invalid syntax")));
    }

    @Test
    void getFacetsSuccess() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(MEMBER_PREFIX_PATH + ID_FILTER + MEMBER_SUFIX_PATH)
                                .param("facets", "member_id_type,uniprot_member_id_type")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(6)))
                .andExpect(jsonPath("$.facets.size()", is(2)))
                .andExpect(jsonPath("$.facets[0].label", is("Member Types")))
                .andExpect(jsonPath("$.facets[0].name", is("member_id_type")))
                .andExpect(jsonPath("$.facets[0].values.size()", is(2)))
                .andExpect(
                        jsonPath(
                                "$.facets[0].values[*].value", hasItems("uniprotkb_id", "uniparc")))
                .andExpect(jsonPath("$.facets[0].values[*].count", hasItems(4, 2)))
                .andExpect(jsonPath("$.facets[1].label", is("UniProtKB Member Types")))
                .andExpect(jsonPath("$.facets[1].name", is("uniprot_member_id_type")))
                .andExpect(jsonPath("$.facets[1].values.size()", is(2)))
                .andExpect(
                        jsonPath(
                                "$.facets[1].values[*].value",
                                hasItems(
                                        "uniprotkb_reviewed_swissprot",
                                        "uniprotkb_unreviewed_trembl")))
                .andExpect(jsonPath("$.facets[1].values[*].count", hasItems(3, 1)));
    }

    @Test
    void invalidFacetsBadRequest() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(MEMBER_PREFIX_PATH + ID_50 + MEMBER_SUFIX_PATH)
                                .param("facets", "invalid")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.url", not(emptyOrNullString())))
                .andExpect(jsonPath("$.messages.size()", is(1)))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains(
                                        "Invalid facet name 'invalid'. Expected value can be [member_id_type, uniprot_member_id_type].")));
    }

    @ParameterizedTest(name = "[{index}] search with facetName {0}")
    @MethodSource("getAllFacetFieldsArguments")
    void getMembersSuccessSizeZero(String facetField) throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(MEMBER_PREFIX_PATH + ID_100 + MEMBER_SUFIX_PATH)
                                .param("size", "0")
                                .param("facets", facetField)
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(0)))
                .andExpect(jsonPath("$.facets.size()", greaterThan(0)))
                .andExpect(jsonPath("$.facets.*.name", contains(facetField)))
                .andExpect(jsonPath("$.facets[0].values.size()", greaterThan(0)))
                .andExpect(jsonPath("$.facets[0].values.*.count", hasItem(greaterThan(0))));
    }

    @Test
    void getMemberFailureSizeMinusOne() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(MEMBER_PREFIX_PATH + ID_100 + MEMBER_SUFIX_PATH)
                                .param("size", "-1")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains("'size' must be greater than or equal to 0")));
    }

    @NotNull
    private UniRefEntry getUniRefEntryForFilter() {
        UniRefEntryBuilder builder = new UniRefEntryBuilder();
        builder.id(ID_FILTER);
        builder.name("Cluster Name");
        builder.commonTaxon(
                new OrganismBuilder().taxonId(9606L).scientificName("Homo Sapiens").build());
        builder.entryType(UniRefType.UniRef50);

        RepresentativeMemberBuilder repBuilder =
                RepresentativeMemberBuilder.from(createReprestativeMember(30));
        repBuilder.uniparcId(null);
        repBuilder.memberIdType(UniRefMemberIdType.UNIPROTKB_SWISSPROT);
        repBuilder.memberId("CDC6_HUMAN");
        builder.representativeMember(repBuilder.build());

        builder.membersAdd(
                UniRefMemberBuilder.from(createMember(40))
                        .memberIdType(UniRefMemberIdType.UNIPROTKB_SWISSPROT)
                        .memberId("CDC7_HUMAN")
                        .uniparcId(null)
                        .build());
        builder.membersAdd(
                UniRefMemberBuilder.from(createMember(50))
                        .memberIdType(UniRefMemberIdType.UNIPROTKB_SWISSPROT)
                        .memberId("CDC8_HUMAN")
                        .uniparcId(null)
                        .build());

        builder.membersAdd(
                UniRefMemberBuilder.from(createMember(60))
                        .memberIdType(UniRefMemberIdType.UNIPARC)
                        .memberId("UP123456788")
                        .accessionsSet(new ArrayList<>())
                        .build());
        builder.membersAdd(
                UniRefMemberBuilder.from(createMember(70))
                        .memberIdType(UniRefMemberIdType.UNIPARC)
                        .memberId("UP123456789")
                        .accessionsSet(new ArrayList<>())
                        .build());

        builder.membersAdd(
                UniRefMemberBuilder.from(createMember(80))
                        .memberIdType(UniRefMemberIdType.UNIPROTKB_TREMBL)
                        .uniparcId(null)
                        .build());
        builder.memberCount(6);
        return builder.build();
    }

    private Stream<Arguments> getAllFacetFieldsArguments() {
        return this.uniRefEntryFacetConfig.getFacetNames().stream().map(Arguments::of);
    }
}
