package org.uniprot.api.proteome.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.proteome.ProteomeRestApplication;
import org.uniprot.api.proteome.repository.GeneCentricQueryRepository;
import org.uniprot.api.rest.controller.AbstractGetByIdControllerIT;
import org.uniprot.api.rest.controller.param.ContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdParameter;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetIdContentTypeParamResolver;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetIdParameterResolver;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.core.json.parser.proteome.ProteomeJsonConfig;
import org.uniprot.core.proteome.CanonicalProtein;
import org.uniprot.core.proteome.Protein;
import org.uniprot.core.proteome.impl.CanonicalProteinBuilder;
import org.uniprot.core.proteome.impl.ProteinBuilder;
import org.uniprot.core.uniprotkb.UniProtKBEntryType;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.proteome.GeneCentricDocument;
import org.uniprot.store.search.document.proteome.GeneCentricDocument.GeneCentricDocumentBuilder;
import org.uniprot.store.search.field.GeneCentricField;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author jluo
 * @date: 14 Jun 2019
 */
@ContextConfiguration(
        classes = {
            DataStoreTestConfig.class,
            ProteomeRestApplication.class,
            ErrorHandlerConfig.class
        })
@ActiveProfiles(profiles = "offline")
@WebMvcTest(GeneCentricController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
            GeneCentricGetIdControllerIT.GeneCentricGetIdParameterResolver.class,
            GeneCentricGetIdControllerIT.GeneCentricGetIdContentTypeParamResolver.class
        })
public class GeneCentricGetIdControllerIT extends AbstractGetByIdControllerIT {
    private static final String ACCESSION = "P21312";

    @Autowired private GeneCentricQueryRepository repository;

    @Override
    protected DataStoreManager.StoreType getStoreType() {
        return DataStoreManager.StoreType.GENECENTRIC;
    }

    @Override
    protected SolrCollection getSolrCollection() {
        return SolrCollection.genecentric;
    }

    @Override
    protected SolrQueryRepository getRepository() {
        return repository;
    }

    @Override
    protected void saveEntry() {
        CanonicalProtein entry = create();
        GeneCentricDocumentBuilder builder = GeneCentricDocument.builder();
        builder.accession(ACCESSION)
                .accessions(Arrays.asList(ACCESSION, "P21912", "P31912"))
                .upid("UP000005641");
        builder.geneCentricStored(getBinary(entry));
        getStoreManager().saveDocs(getStoreType(), builder.build());
    }

    private ByteBuffer getBinary(CanonicalProtein entry) {
        try {
            return ByteBuffer.wrap(
                    ProteomeJsonConfig.getInstance()
                            .getFullObjectMapper()
                            .writeValueAsBytes(entry));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to parse TaxonomyEntry to binary json: ", e);
        }
    }

    private CanonicalProtein create() {
        Protein protein =
                new ProteinBuilder()
                        .accession(ACCESSION)
                        .entryType(UniProtKBEntryType.SWISSPROT)
                        .geneName("some gene")
                        .geneNameType(org.uniprot.core.proteome.GeneNameType.ENSEMBL)
                        .sequenceLength(324)
                        .build();

        Protein protein2 =
                new ProteinBuilder()
                        .accession("P21912")
                        .entryType(UniProtKBEntryType.SWISSPROT)
                        .geneName("some gene1")
                        .geneNameType(org.uniprot.core.proteome.GeneNameType.ENSEMBL)
                        .sequenceLength(334)
                        .build();
        Protein protein3 =
                new ProteinBuilder()
                        .accession("P31912")
                        .entryType(UniProtKBEntryType.TREMBL)
                        .geneName("some gene3")
                        .geneNameType(org.uniprot.core.proteome.GeneNameType.OLN)
                        .sequenceLength(434)
                        .build();
        CanonicalProteinBuilder builder = new CanonicalProteinBuilder();

        return builder.canonicalProtein(protein)
                .relatedProteinsAdd(protein2)
                .relatedProteinsAdd(protein3)
                .build();
    }

    @Override
    protected String getIdRequestPath() {
        return "/genecentric/";
    }

    static class GeneCentricGetIdParameterResolver extends AbstractGetIdParameterResolver {

        @Override
        public GetIdParameter validIdParameter() {
            return GetIdParameter.builder()
                    .id(ACCESSION)
                    .resultMatcher(jsonPath("$.*.accession.value", contains(ACCESSION)))
                    //
                    // .resultMatcher(jsonPath("$.scientificName",is("scientific")))
                    //		                    .resultMatcher(jsonPath("$.commonName",is("common")))
                    //		                    .resultMatcher(jsonPath("$.mnemonic",is("mnemonic")))
                    //		                    .resultMatcher(jsonPath("$.links",contains("link")))
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
                                    contains(
                                            "The 'accession' value has invalid format. It should be a valid UniProtKB accession")))
                    .build();
        }

        @Override
        public GetIdParameter nonExistentIdParameter() {
            return GetIdParameter.builder()
                    .id("P21910")
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(jsonPath("$.messages.*", contains("Resource not found")))
                    .build();
        }

        @Override
        public GetIdParameter withFilterFieldsParameter() {
            return GetIdParameter.builder()
                    .id(ACCESSION)
                    .fields("accession_id")
                    .resultMatcher(jsonPath("$.*.accession.value", contains(ACCESSION)))
                    //
                    // .resultMatcher(jsonPath("$.scientificName",is("scientific")))
                    //		                    .resultMatcher(jsonPath("$.commonName").doesNotExist())
                    //		                    .resultMatcher(jsonPath("$.mnemonic").doesNotExist())
                    //		                    .resultMatcher(jsonPath("$.links").doesNotExist())
                    .build();
        }

        @Override
        public GetIdParameter withInvalidFilterParameter() {
            return GetIdParameter.builder()
                    .id(ACCESSION)
                    .fields("invalid")
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    contains("Invalid fields parameter value 'invalid'")))
                    .build();
        }

        @Override
        public GetIdParameter withValidResponseFieldsOrderParameter() {
            return GetIdParameter.builder()
                    .id(ACCESSION)
                    .resultMatcher(
                            result -> {
                                String contentAsString = result.getResponse().getContentAsString();
                                try {
                                    Map<String, Object> responseMap =
                                            new ObjectMapper()
                                                    .readValue(
                                                            contentAsString, LinkedHashMap.class);
                                    List<String> actualList = new ArrayList<>(responseMap.keySet());
                                    List<String> expectedList = getFieldsInOrder();
                                    Assertions.assertEquals(expectedList.size(), actualList.size());
                                    Assertions.assertEquals(expectedList, actualList);
                                } catch (IOException e) {
                                    Assertions.fail(e.getMessage());
                                }
                            })
                    .build();
        }

        private List<String> getFieldsInOrder() {
            List<String> fields = new LinkedList<>();
            fields.add(GeneCentricField.ResultFields.accession_id.getJavaFieldName());
            fields.add(GeneCentricField.ResultFields.related_accession.getJavaFieldName());
            return fields;
        }
    }

    static class GeneCentricGetIdContentTypeParamResolver
            extends AbstractGetIdContentTypeParamResolver {

        @Override
        public GetIdContentTypeParam idSuccessContentTypesParam() {
            return GetIdContentTypeParam.builder()
                    .id(ACCESSION)
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(
                                            jsonPath("$.*.accession.value", contains(ACCESSION)))
                                    //
                                    // .resultMatcher(jsonPath("$.scientificName",is("scientific")))
                                    //
                                    // .resultMatcher(jsonPath("$.commonName",is("common")))
                                    //
                                    // .resultMatcher(jsonPath("$.mnemonic",is("mnemonic")))
                                    //
                                    // .resultMatcher(jsonPath("$.links",contains("link")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_XML)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "accession=\""
                                                                            + ACCESSION
                                                                            + "\"")))
                                    //
                                    // .resultMatcher(jsonPath("$.scientificName",is("scientific")))
                                    //
                                    // .resultMatcher(jsonPath("$.commonName",is("common")))
                                    //
                                    // .resultMatcher(jsonPath("$.mnemonic",is("mnemonic")))
                                    //
                                    // .resultMatcher(jsonPath("$.links",contains("link")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(content().string(containsString(ACCESSION)))
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
                                                            "The 'accession' value has invalid format. It should be a valid UniProtKB accession")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_XML)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "The 'accession' value has invalid format. It should be a valid UniProtKB accession")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(content().string(isEmptyString()))
                                    .build())
                    .build();
        }
    }
}
