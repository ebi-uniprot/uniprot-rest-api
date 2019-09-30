package org.uniprot.api.proteome.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.nio.ByteBuffer;
import java.util.Arrays;

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
import org.uniprot.core.proteome.builder.CanonicalProteinBuilder;
import org.uniprot.core.proteome.builder.ProteinBuilder;
import org.uniprot.core.uniprot.UniProtEntryType;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.proteome.GeneCentricDocument;
import org.uniprot.store.search.document.proteome.GeneCentricDocument.GeneCentricDocumentBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;

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
                ProteinBuilder.newInstance()
                        .accession(ACCESSION)
                        .entryType(UniProtEntryType.SWISSPROT)
                        .geneName("some gene")
                        .geneNameType(org.uniprot.core.proteome.GeneNameType.ENSEMBL)
                        .sequenceLength(324)
                        .build();

        Protein protein2 =
                ProteinBuilder.newInstance()
                        .accession("P21912")
                        .entryType(UniProtEntryType.SWISSPROT)
                        .geneName("some gene1")
                        .geneNameType(org.uniprot.core.proteome.GeneNameType.ENSEMBL)
                        .sequenceLength(334)
                        .build();
        Protein protein3 =
                ProteinBuilder.newInstance()
                        .accession("P31912")
                        .entryType(UniProtEntryType.TREMBL)
                        .geneName("some gene3")
                        .geneNameType(org.uniprot.core.proteome.GeneNameType.OLN)
                        .sequenceLength(434)
                        .build();
        CanonicalProteinBuilder builder = CanonicalProteinBuilder.newInstance();

        return builder.canonicalProtein(protein)
                .addRelatedProtein(protein2)
                .addRelatedProtein(protein3)
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
