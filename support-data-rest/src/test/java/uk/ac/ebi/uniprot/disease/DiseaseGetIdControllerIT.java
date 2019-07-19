package uk.ac.ebi.uniprot.disease;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.ebi.uniprot.api.DataStoreTestConfig;
import uk.ac.ebi.uniprot.api.disease.DiseaseController;
import uk.ac.ebi.uniprot.api.rest.controller.AbstractGetByIdControllerIT;
import uk.ac.ebi.uniprot.api.rest.controller.param.ContentTypeParam;
import uk.ac.ebi.uniprot.api.rest.controller.param.GetIdContentTypeParam;
import uk.ac.ebi.uniprot.api.rest.controller.param.GetIdParameter;
import uk.ac.ebi.uniprot.api.rest.controller.param.resolver.AbstractGetIdContentTypeParamResolver;
import uk.ac.ebi.uniprot.api.rest.controller.param.resolver.AbstractGetIdParameterResolver;
import uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType;
import uk.ac.ebi.uniprot.api.support_data.SupportDataApplication;
import uk.ac.ebi.uniprot.cv.disease.CrossReference;
import uk.ac.ebi.uniprot.cv.disease.Disease;
import uk.ac.ebi.uniprot.cv.keyword.Keyword;
import uk.ac.ebi.uniprot.cv.keyword.impl.KeywordImpl;
import uk.ac.ebi.uniprot.domain.builder.DiseaseBuilder;
import uk.ac.ebi.uniprot.indexer.DataStoreManager;
import uk.ac.ebi.uniprot.json.parser.disease.DiseaseJsonConfig;
import uk.ac.ebi.uniprot.search.document.disease.DiseaseDocument;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ContextConfiguration(classes = {DataStoreTestConfig.class, SupportDataApplication.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(DiseaseController.class)
@ExtendWith(value = {SpringExtension.class, DiseaseGetIdControllerIT.DiseaseGetIdParameterResolver.class,
        DiseaseGetIdControllerIT.DiseaseGetIdContentTypeParamResolver.class})
public class DiseaseGetIdControllerIT extends AbstractGetByIdControllerIT {

    private static final String ACCESSION = "DI-04860";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataStoreManager storeManager;

    @Override
    public MockMvc getMockMvc() {
        return mockMvc;
    }

    @Override
    public String getIdRequestPath() {
        return "/disease/";
    }

    @Override
    public void saveEntry() {

        DiseaseBuilder diseaseBuilder = new DiseaseBuilder();
        Keyword keyword = new KeywordImpl("Mental retardation", "KW-0991");
        CrossReference xref1 = new CrossReference("MIM", "617140", Arrays.asList("phenotype"));
        CrossReference xref2 = new CrossReference("MedGen", "CN238690");
        CrossReference xref3 = new CrossReference("MeSH", "D000015");
        CrossReference xref4 = new CrossReference("MeSH", "D008607");
        Disease diseaseEntry = diseaseBuilder.id("ZTTK syndrome")
                .accession(ACCESSION)
                .acronym("ZTTKS")
                .definition("An autosomal dominant syndrome characterized by intellectual disability, developmental delay, malformations of the cerebral cortex, epilepsy, vision problems, musculo-skeletal abnormalities, and congenital malformations.")
                .alternativeNames(Arrays.asList("Zhu-Tokita-Takenouchi-Kim syndrome", "ZTTK multiple congenital anomalies-mental retardation syndrome"))
                .crossReferences(Arrays.asList(xref1, xref2, xref3, xref4))
                .keywords(keyword)
                .reviewedProteinCount(1L)
                .unreviewedProteinCount(0L)
                .build();

        List<String> kwIds;
        if (diseaseEntry.getKeywords() != null) {
            kwIds = diseaseEntry.getKeywords().stream().map(kw -> kw.getId()).collect(Collectors.toList());
        } else {
            kwIds = new ArrayList<>();
        }
        // name is a combination of id, acronym, definition, synonyms, keywords
        List<String> name = Stream.concat(Stream.concat(Stream.of(diseaseEntry.getId(), diseaseEntry.getAcronym(), diseaseEntry.getDefinition()),
                kwIds.stream()),
                diseaseEntry.getAlternativeNames().stream())
                .collect(Collectors.toList());
        // content is name + accession
        List<String> content = new ArrayList<>();
        content.addAll(name);
        content.add(diseaseEntry.getAccession());
        DiseaseDocument document = DiseaseDocument.builder()
                .accession(ACCESSION)
                .name(name)
                .content(content)
                .diseaseObj(getDiseaseBinary(diseaseEntry))
                .build();

        this.storeManager.saveDocs(DataStoreManager.StoreType.DISEASE, document);
    }

    private ByteBuffer getDiseaseBinary(Disease entry) {
        try {
            return ByteBuffer.wrap(DiseaseJsonConfig.getInstance().getFullObjectMapper().writeValueAsBytes(entry));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to parse Disease to binary json: ", e);
        }
    }

    static class DiseaseGetIdParameterResolver extends AbstractGetIdParameterResolver {

        @Override
        public GetIdParameter validIdParameter() {
            return GetIdParameter.builder().id(ACCESSION)
                    .resultMatcher(jsonPath("$.accession", is(ACCESSION)))
                    .resultMatcher(jsonPath("$.acronym", is("ZTTKS")))
                    .resultMatcher(jsonPath("$.id", is("ZTTK syndrome")))
                    .resultMatcher(jsonPath("$.definition", containsString("characterized by intellectual disability")))
                    .resultMatcher(jsonPath("$.unreviewedProteinCount", is(0)))
                    .resultMatcher(jsonPath("$.reviewedProteinCount", is(1)))
                    .resultMatcher(jsonPath("$.keywords.length()", is(1)))
                    .resultMatcher(jsonPath("$.keywords[0].id", is("Mental retardation")))
                    .resultMatcher(jsonPath("$.keywords[0].accession", is("KW-0991")))
                    .resultMatcher(jsonPath("$.alternativeNames.length()", is(2)))
                    .resultMatcher(jsonPath("$.alternativeNames", containsInAnyOrder(  "Zhu-Tokita-Takenouchi-Kim syndrome",
                            "ZTTK multiple congenital anomalies-mental retardation syndrome")))
                    .resultMatcher(jsonPath("$.crossReferences.length()", is(4)))
                    .build();
        }

        @Override
        public GetIdParameter invalidIdParameter() {
            return GetIdParameter.builder().id("INVALID")
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(jsonPath("$.messages.*", contains("Invalid accession format. Expected DI-xxxxx")))
                    .build();
        }

        @Override
        public GetIdParameter nonExistentIdParameter() {
            return GetIdParameter.builder().id("DI-00000")
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(jsonPath("$.messages.*", contains("Resource not found")))
                    .build();
        }

        @Override
        public GetIdParameter withFilterFieldsParameter() {
            return GetIdParameter.builder().id(ACCESSION).fields("id,accession,reviewed_protein_count")
                    .resultMatcher(jsonPath("$.accession", is(ACCESSION)))
                    .resultMatcher(jsonPath("$.id", is("ZTTK syndrome")))
                    .resultMatcher(jsonPath("$.reviewedProteinCount", is(1)))
                    .resultMatcher(jsonPath("$.alternativeNames").doesNotExist())
                    .resultMatcher(jsonPath("$.unreviewedProteinCount").doesNotExist())
                    .resultMatcher(jsonPath("$.acronym").doesNotExist())
                    .build();
        }

        @Override
        public GetIdParameter withInvalidFilterParameter() {
            return GetIdParameter.builder().id(ACCESSION).fields("invalid")
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(jsonPath("$.messages.*", contains("Invalid fields parameter value 'invalid'")))
                    .build();
        }
    }

    static class DiseaseGetIdContentTypeParamResolver extends AbstractGetIdContentTypeParamResolver {

        @Override
        public GetIdContentTypeParam idSuccessContentTypesParam() {
            return GetIdContentTypeParam.builder()
                    .id(ACCESSION)
                    .contentTypeParam(ContentTypeParam.builder()
                            .contentType(MediaType.APPLICATION_JSON)
                            .resultMatcher(jsonPath("$.accession", is(ACCESSION)))
                            .resultMatcher(jsonPath("$.id", is("ZTTK syndrome")))
                            .resultMatcher(jsonPath("$.acronym", is("ZTTKS")))
                            .resultMatcher(jsonPath("$.unreviewedProteinCount", is(0)))
                            .resultMatcher(jsonPath("$.reviewedProteinCount", is(1)))
                            .resultMatcher(jsonPath("$.definition", containsString("characterized by intellectual disability")))
                            .build())
                    .contentTypeParam(ContentTypeParam.builder()
                            .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                            .resultMatcher(content().string(containsString("ZTTK syndrome")))
                            .build())
                    .contentTypeParam(ContentTypeParam.builder()
                            .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                            .resultMatcher(content().string(containsString("Name\tDisease ID\tMnemonic\tDescription")))
                            .resultMatcher(content().string(containsString("ZTTK syndrome\tDI-04860\tZTTKS\tAn autosomal dominant syndrome characterized by intellectual disability, developmental delay, malformations of the cerebral cortex, epilepsy, vision problems, musculo-skeletal abnormalities, and congenital malformations.")))
                            .build())
                    .contentTypeParam(ContentTypeParam.builder()
                            .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                            .resultMatcher(content().contentType(UniProtMediaType.XLS_MEDIA_TYPE))
                            .build())
                    .build();
        }

        @Override
        public GetIdContentTypeParam idBadRequestContentTypesParam() {
            return GetIdContentTypeParam.builder()
                    .id("INVALID")
                    .contentTypeParam(ContentTypeParam.builder()
                            .contentType(MediaType.APPLICATION_JSON)
                            .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                            .resultMatcher(jsonPath("$.messages.*", contains("Invalid accession format. Expected DI-xxxxx")))
                            .build())
                    .contentTypeParam(ContentTypeParam.builder()
                            .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                            .resultMatcher(content().string(isEmptyString()))
                            .build())
                    .contentTypeParam(ContentTypeParam.builder()
                            .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                            .resultMatcher(content().string(isEmptyString()))
                            .build())
                    .contentTypeParam(ContentTypeParam.builder()
                            .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                            .resultMatcher(content().string(isEmptyString()))
                            .build())
                    .build();
        }
    }
}
