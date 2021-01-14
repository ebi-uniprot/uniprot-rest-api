package org.uniprot.api.support.data.disease.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.uniprot.api.support.data.DataStoreTestConfig;
import org.uniprot.api.support.data.SupportDataRestApplication;
import org.uniprot.api.support.data.disease.repository.DiseaseRepository;
import org.uniprot.core.cv.disease.DiseaseCrossReference;
import org.uniprot.core.cv.disease.DiseaseEntry;
import org.uniprot.core.cv.disease.impl.DiseaseCrossReferenceBuilder;
import org.uniprot.core.cv.disease.impl.DiseaseEntryBuilder;
import org.uniprot.core.cv.keyword.KeywordId;
import org.uniprot.core.cv.keyword.impl.KeywordIdBuilder;
import org.uniprot.core.json.parser.disease.DiseaseJsonConfig;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.disease.DiseaseDocument;

import com.fasterxml.jackson.core.JsonProcessingException;

@ContextConfiguration(classes = {DataStoreTestConfig.class, SupportDataRestApplication.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(DiseaseController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
            DiseaseGetIdControllerIT.DiseaseGetIdParameterResolver.class,
            DiseaseGetIdControllerIT.DiseaseGetIdContentTypeParamResolver.class
        })
public class DiseaseGetIdControllerIT extends AbstractGetByIdControllerIT {

    private static final String ACCESSION = "DI-04860";

    @Autowired private DiseaseRepository repository;

    @Override
    protected DataStoreManager.StoreType getStoreType() {
        return DataStoreManager.StoreType.DISEASE;
    }

    @Override
    protected SolrCollection getSolrCollection() {
        return SolrCollection.disease;
    }

    @Override
    protected SolrQueryRepository getRepository() {
        return repository;
    }

    @Override
    protected String getIdRequestPath() {
        return "/disease/";
    }

    @Override
    protected void saveEntry() {

        DiseaseEntryBuilder diseaseBuilder = new DiseaseEntryBuilder();
        KeywordId keyword = new KeywordIdBuilder().name("Mental retardation").id("KW-0991").build();
        DiseaseCrossReference xref1 =
                new DiseaseCrossReferenceBuilder()
                        .databaseType("MIM")
                        .id("617140")
                        .propertiesAdd("phenotype")
                        .build();
        DiseaseCrossReference xref2 =
                new DiseaseCrossReferenceBuilder().databaseType("MedGen").id("CN238690").build();
        DiseaseCrossReference xref3 =
                new DiseaseCrossReferenceBuilder().databaseType("MeSH").id("D000015").build();
        DiseaseCrossReference xref4 =
                DiseaseCrossReferenceBuilder.from(xref3).id("D008607").build();
        DiseaseEntry diseaseEntry =
                diseaseBuilder
                        .name("ZTTK syndrome")
                        .id(ACCESSION)
                        .acronym("ZTTKS")
                        .definition(
                                "An autosomal dominant syndrome characterized by intellectual disability, developmental delay, malformations of the cerebral cortex, epilepsy, vision problems, musculo-skeletal abnormalities, and congenital malformations.")
                        .alternativeNamesSet(
                                Arrays.asList(
                                        "Zhu-Tokita-Takenouchi-Kim syndrome",
                                        "ZTTK multiple congenital anomalies-mental retardation syndrome"))
                        .crossReferencesSet(Arrays.asList(xref1, xref2, xref3, xref4))
                        .keywordsAdd(keyword)
                        .reviewedProteinCount(1L)
                        .unreviewedProteinCount(0L)
                        .build();

        List<String> kwIds;
        if (diseaseEntry.getKeywords() != null) {
            kwIds =
                    diseaseEntry.getKeywords().stream()
                            .map(KeywordId::getName)
                            .collect(Collectors.toList());
        } else {
            kwIds = new ArrayList<>();
        }
        // name is a combination of id, acronym, definition, synonyms, keywords
        List<String> name =
                Stream.concat(
                                Stream.concat(
                                        Stream.of(
                                                diseaseEntry.getName(),
                                                diseaseEntry.getAcronym(),
                                                diseaseEntry.getDefinition()),
                                        kwIds.stream()),
                                diseaseEntry.getAlternativeNames().stream())
                        .collect(Collectors.toList());
        // content is name + accession
        List<String> content = new ArrayList<>(name);
        content.add(diseaseEntry.getId());
        DiseaseDocument document =
                DiseaseDocument.builder()
                        .id(ACCESSION)
                        .name(name)
                        .content(content)
                        .diseaseObj(getDiseaseBinary(diseaseEntry))
                        .build();

        this.getStoreManager().saveDocs(DataStoreManager.StoreType.DISEASE, document);
    }

    private ByteBuffer getDiseaseBinary(DiseaseEntry entry) {
        try {
            return ByteBuffer.wrap(
                    DiseaseJsonConfig.getInstance().getFullObjectMapper().writeValueAsBytes(entry));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to parse DiseaseEntry to binary json: ", e);
        }
    }

    static class DiseaseGetIdParameterResolver extends AbstractGetIdParameterResolver {

        @Override
        public GetIdParameter validIdParameter() {
            return GetIdParameter.builder()
                    .id(ACCESSION)
                    .resultMatcher(jsonPath("$.id", is(ACCESSION)))
                    .resultMatcher(jsonPath("$.acronym", is("ZTTKS")))
                    .resultMatcher(jsonPath("$.name", is("ZTTK syndrome")))
                    .resultMatcher(
                            jsonPath(
                                    "$.definition",
                                    containsString("characterized by intellectual disability")))
                    .resultMatcher(jsonPath("$.unreviewedProteinCount", is(0)))
                    .resultMatcher(jsonPath("$.reviewedProteinCount", is(1)))
                    .resultMatcher(jsonPath("$.keywords.length()", is(1)))
                    .resultMatcher(jsonPath("$.keywords[0].name", is("Mental retardation")))
                    .resultMatcher(jsonPath("$.keywords[0].id", is("KW-0991")))
                    .resultMatcher(jsonPath("$.alternativeNames.length()", is(2)))
                    .resultMatcher(
                            jsonPath(
                                    "$.alternativeNames",
                                    containsInAnyOrder(
                                            "Zhu-Tokita-Takenouchi-Kim syndrome",
                                            "ZTTK multiple congenital anomalies-mental retardation syndrome")))
                    .resultMatcher(jsonPath("$.crossReferences.length()", is(4)))
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
                                            "The disease id value has invalid format. It should match the regular expression 'DI-[0-9]{5}'")))
                    .build();
        }

        @Override
        public GetIdParameter nonExistentIdParameter() {
            return GetIdParameter.builder()
                    .id("DI-00000")
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(jsonPath("$.messages.*", contains("Resource not found")))
                    .build();
        }

        @Override
        public GetIdParameter withFilterFieldsParameter() {
            return GetIdParameter.builder()
                    .id(ACCESSION)
                    .fields("name,id,reviewed_protein_count")
                    .resultMatcher(jsonPath("$.id", is(ACCESSION)))
                    .resultMatcher(jsonPath("$.name", is("ZTTK syndrome")))
                    .resultMatcher(jsonPath("$.reviewedProteinCount", is(1)))
                    .resultMatcher(jsonPath("$.alternativeNames").doesNotExist())
                    .resultMatcher(jsonPath("$.unreviewedProteinCount").doesNotExist())
                    .resultMatcher(jsonPath("$.acronym").doesNotExist())
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

    static class DiseaseGetIdContentTypeParamResolver
            extends AbstractGetIdContentTypeParamResolver {

        @Override
        public GetIdContentTypeParam idSuccessContentTypesParam() {

            String fmtStr = "format-version: 1.2";
            String defaultNSStr = "default-namespace: uniprot:diseases";
            String termStr =
                    "[Term]\n"
                            + "id: DI-04860\n"
                            + "name: ZTTK syndrome\n"
                            + "def: \"An autosomal dominant syndrome characterized by intellectual disability, developmental delay, malformations of the cerebral cortex, epilepsy, vision problems, musculo-skeletal abnormalities, and congenital malformations.\" []\n"
                            + "synonym: \"Zhu-Tokita-Takenouchi-Kim syndrome\" [UniProt]\n"
                            + "synonym: \"ZTTK multiple congenital anomalies-mental retardation syndrome\" [UniProt]\n"
                            + "xref: MedGen:CN238690\n"
                            + "xref: MeSH:D000015\n"
                            + "xref: MeSH:D008607\n"
                            + "xref: MIM:617140 \"phenotype\"";

            return GetIdContentTypeParam.builder()
                    .id(ACCESSION)
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(jsonPath("$.id", is(ACCESSION)))
                                    .resultMatcher(jsonPath("$.name", is("ZTTK syndrome")))
                                    .resultMatcher(jsonPath("$.acronym", is("ZTTKS")))
                                    .resultMatcher(jsonPath("$.unreviewedProteinCount", is(0)))
                                    .resultMatcher(jsonPath("$.reviewedProteinCount", is(1)))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.definition",
                                                    containsString(
                                                            "characterized by intellectual disability")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(content().string(containsString(ACCESSION)))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "Name\tDiseaseEntry ID\tMnemonic\tDescription")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "ZTTK syndrome\tDI-04860\tZTTKS\tAn autosomal dominant syndrome characterized by intellectual disability, developmental delay, malformations of the cerebral cortex, epilepsy, vision problems, musculo-skeletal abnormalities, and congenital malformations.")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                                    .resultMatcher(
                                            content().contentType(UniProtMediaType.XLS_MEDIA_TYPE))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.OBO_MEDIA_TYPE)
                                    .resultMatcher(
                                            content().contentType(UniProtMediaType.OBO_MEDIA_TYPE))
                                    .resultMatcher(content().string(containsString(fmtStr)))
                                    .resultMatcher(content().string(containsString(defaultNSStr)))
                                    .resultMatcher(content().string(containsString(termStr)))
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
                                                            "The disease id value has invalid format. It should match the regular expression 'DI-[0-9]{5}'")))
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
                                    .contentType(UniProtMediaType.OBO_MEDIA_TYPE)
                                    .resultMatcher(content().string(isEmptyString()))
                                    .build())
                    .build();
        }
    }
}
