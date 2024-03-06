package org.uniprot.api.proteome.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.*;

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
import org.uniprot.core.genecentric.GeneCentricEntry;
import org.uniprot.core.genecentric.Protein;
import org.uniprot.core.genecentric.impl.GeneCentricEntryBuilder;
import org.uniprot.core.genecentric.impl.ProteinBuilder;
import org.uniprot.core.json.parser.genecentric.GeneCentricJsonConfig;
import org.uniprot.core.uniprotkb.ProteinExistence;
import org.uniprot.core.uniprotkb.UniProtKBEntryType;
import org.uniprot.core.uniprotkb.taxonomy.impl.OrganismBuilder;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.genecentric.GeneCentricDocument;
import org.uniprot.store.search.document.genecentric.GeneCentricDocument.GeneCentricDocumentBuilder;
import org.uniprot.store.search.document.genecentric.GeneCentricDocumentConverter;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author jluo
 * @date: 14 Jun 2019
 */
@ContextConfiguration(classes = {ProteomeRestApplication.class, ErrorHandlerConfig.class})
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
        GeneCentricEntry entry = create();
        GeneCentricDocumentBuilder builder = GeneCentricDocument.builder();
        builder.accession(ACCESSION)
                .accessions(Arrays.asList(ACCESSION, "P21912", "P31912"))
                .upid("UP000005641");
        builder.geneCentricStored(getBinary(entry));
        getStoreManager().saveDocs(getStoreType(), builder.build());
    }

    private byte[] getBinary(GeneCentricEntry entry) {
        ObjectMapper mapper = GeneCentricJsonConfig.getInstance().getFullObjectMapper();
        GeneCentricDocumentConverter converter = new GeneCentricDocumentConverter(mapper);
        return converter.getStoredGeneCentricEntry(entry);
    }

    private GeneCentricEntry create() {
        Protein protein =
                new ProteinBuilder()
                        .id(ACCESSION)
                        .uniProtkbId("uniprotkb_id")
                        .entryType(UniProtKBEntryType.SWISSPROT)
                        .proteinName("protein name")
                        .geneName("some gene")
                        .organism(
                                new OrganismBuilder()
                                        .taxonId(9606L)
                                        .scientificName("Human")
                                        .build())
                        .sequence("AAAAA")
                        .sequenceVersion(2)
                        .proteinExistence(ProteinExistence.PROTEIN_LEVEL)
                        .build();

        Protein protein2 =
                new ProteinBuilder()
                        .id("P21912")
                        .uniProtkbId("uniprotkb_id")
                        .entryType(UniProtKBEntryType.SWISSPROT)
                        .proteinName("protein name")
                        .geneName("some gene1")
                        .organism(
                                new OrganismBuilder()
                                        .taxonId(9606L)
                                        .scientificName("Human")
                                        .build())
                        .sequence("BBBBB")
                        .sequenceVersion(2)
                        .proteinExistence(ProteinExistence.PREDICTED)
                        .build();
        Protein protein3 =
                new ProteinBuilder()
                        .id("P31912")
                        .uniProtkbId("uniprotkb_id")
                        .entryType(UniProtKBEntryType.TREMBL)
                        .proteinName("protein name")
                        .geneName("some gene3")
                        .organism(
                                new OrganismBuilder()
                                        .taxonId(9606L)
                                        .scientificName("Human")
                                        .build())
                        .sequence("CCCCC")
                        .sequenceVersion(2)
                        .proteinExistence(ProteinExistence.UNCERTAIN)
                        .build();
        GeneCentricEntryBuilder builder = new GeneCentricEntryBuilder();

        return builder.canonicalProtein(protein)
                .proteomeId("UP000000554")
                .relatedProteinsAdd(protein2)
                .relatedProteinsAdd(protein3)
                .build();
    }

    @Override
    protected String getIdRequestPath() {
        return "/genecentric/{accession}";
    }

    static class GeneCentricGetIdParameterResolver extends AbstractGetIdParameterResolver {

        @Override
        public GetIdParameter validIdParameter() {
            return GetIdParameter.builder()
                    .id(ACCESSION)
                    .resultMatcher(jsonPath("$.canonicalProtein.id", is(ACCESSION)))
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
                                            "The 'accession' value has invalid format. It should be a valid UniProtKB accession")))
                    .build();
        }

        @Override
        public GetIdParameter nonExistentIdParameter() {
            return GetIdParameter.builder()
                    .id("P21910")
                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
                    .resultMatcher(jsonPath("$.messages.*", contains("Resource not found")))
                    .build();
        }

        @Override
        public GetIdParameter withFilterFieldsParameter() {
            return GetIdParameter.builder()
                    .id(ACCESSION)
                    .fields("accession")
                    .resultMatcher(jsonPath("$.canonicalProtein.id", is(ACCESSION)))
                    .resultMatcher(jsonPath("$.canonicalProtein.proteinName").doesNotExist())
                    .resultMatcher(jsonPath("$.canonicalProtein.geneName").doesNotExist())
                    .build();
        }

        @Override
        public GetIdParameter withInvalidFilterParameter() {
            return GetIdParameter.builder()
                    .id(ACCESSION)
                    .fields("invalid")
                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
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
                                    .resultMatcher(jsonPath("$.canonicalProtein.id", is(ACCESSION)))
                                    .resultMatcher(jsonPath("$.proteomeId", is("UP000000554")))
                                    .resultMatcher(jsonPath("$.relatedProteins.size()", is(2)))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_XML)
                                    .resultMatcher(
                                            content().string(containsString("<id>P21312</id>")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "<proteomeId>UP000000554</proteomeId>")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(content().string(containsString(ACCESSION)))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.FASTA_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    ">sp|P21312|uniprotkb_id protein name OS=Human OX=9606 GN=some gene PE=1 SV=2")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    ">tr|P31912|uniprotkb_id protein name OS=Human OX=9606 GN=some gene3 PE=5 SV=2")))
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
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            is(
                                                                    "Error messages\nThe 'accession' value has invalid format. It should be a valid UniProtKB accession")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.FASTA_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            is(
                                                                    "Error messages\nThe 'accession' value has invalid format. It should be a valid UniProtKB accession")))
                                    .build())
                    .build();
        }
    }
}
