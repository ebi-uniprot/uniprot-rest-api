package org.uniprot.api.uniref.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.uniprot.store.indexer.uniref.mockers.UniRefEntryMocker.*;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
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
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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
        UniRefEntry unirefEntry = createEntry(1, 28, UniRefType.UniRef50);
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

    static class UniRefGetIdParameterResolver extends AbstractGetIdParameterResolver {

        @Override
        public GetIdParameter validIdParameter() {
            return GetIdParameter.builder()
                    .id(ID)
                    .resultMatcher(jsonPath("$.id", is(ID)))
                    .resultMatcher(jsonPath("$.id", is(ID)))
                    .resultMatcher(jsonPath("$.name", is(NAME)))
                    .resultMatcher(jsonPath("$.memberCount", is(28)))
                    .resultMatcher(jsonPath("$.updated", is("2019-08-27")))
                    .resultMatcher(jsonPath("$.entryType", is("UniRef50")))
                    .resultMatcher(jsonPath("$.commonTaxonId", is(9606)))
                    .resultMatcher(jsonPath("$.commonTaxon", is("Homo sapiens")))
                    .resultMatcher(jsonPath("$.seedId", is("P12301")))
                    .resultMatcher(jsonPath("$.goTerms.size()", is(3)))
                    .resultMatcher(
                            jsonPath(
                                    "$.representativeMember.memberIdType",
                                    is("UniProtKB Unreviewed (TrEMBL)")))
                    .resultMatcher(jsonPath("$.representativeMember.memberId", is("P12301_HUMAN")))
                    .resultMatcher(
                            jsonPath(
                                    "$.representativeMember.organismName",
                                    is("Homo sapiens (Representative)")))
                    .resultMatcher(jsonPath("$.representativeMember.organismTaxId", is(9600)))
                    .resultMatcher(jsonPath("$.representativeMember.sequenceLength", is(312)))
                    .resultMatcher(
                            jsonPath("$.representativeMember.proteinName", is("some protein name")))
                    .resultMatcher(
                            jsonPath("$.representativeMember.accessions[*]", contains("P12301")))
                    .resultMatcher(jsonPath("$.representativeMember.uniref50Id").doesNotExist())
                    .resultMatcher(
                            jsonPath("$.representativeMember.uniref90Id", is("UniRef90_P03943")))
                    .resultMatcher(
                            jsonPath("$.representativeMember.uniref100Id", is("UniRef100_P03923")))
                    .resultMatcher(
                            jsonPath("$.representativeMember.uniparcId", is("UPI0000083A01")))
                    .resultMatcher(jsonPath("$.representativeMember.seed", is(true)))
                    .resultMatcher(jsonPath("$.representativeMember.sequence").exists())
                    .resultMatcher(jsonPath("$.members[0].memberIdType", is("UniProtKB ID")))
                    .resultMatcher(jsonPath("$.members[0].memberId", is("P32101_HUMAN")))
                    .resultMatcher(jsonPath("$.members[0].organismName", is("Homo sapiens 1")))
                    .resultMatcher(jsonPath("$.members[0].organismTaxId", is(9607)))
                    .resultMatcher(jsonPath("$.members[0].sequenceLength", is(312)))
                    .resultMatcher(jsonPath("$.members[0].proteinName", is("some protein name")))
                    .resultMatcher(jsonPath("$.members[0].accessions[*]", contains("P32101")))
                    .resultMatcher(jsonPath("$.members[0].uniref50Id").doesNotExist())
                    .resultMatcher(jsonPath("$.members[0].uniref90Id", is("UniRef90_P03943")))
                    .resultMatcher(jsonPath("$.members[0].uniref100Id", is("UniRef100_P03923")))
                    .resultMatcher(jsonPath("$.members[0].uniparcId", is("UPI0000083A01")))
                    .resultMatcher(jsonPath("$.members[0].seed").doesNotExist())
                    .resultMatcher(jsonPath("$.members[0].sequence").doesNotExist())
                    .resultMatcher(jsonPath("$.members.size()", is(27)))
                    .build();
        }

        @Override
        public GetIdParameter invalidIdParameter() {
            return GetIdParameter.builder()
                    .id("INVALID")
                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
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
                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
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
                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
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
                                                                    "UniRef50_P03901\tCluster: MoeK5 01\tHomo sapiens\t28\t2019-08-27")))
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
                                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
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
                                    .resultMatcher(content().string(emptyString()))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(content().string(emptyString()))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                                    .resultMatcher(content().string(emptyString()))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.FASTA_MEDIA_TYPE)
                                    .resultMatcher(content().string(emptyString()))
                                    .build())
                    .build();
        }
    }
}
