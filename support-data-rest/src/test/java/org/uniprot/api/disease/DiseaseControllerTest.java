package org.uniprot.api.disease;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.uniprot.api.DataStoreTestConfig;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.support_data.SupportDataApplication;
import org.uniprot.core.cv.disease.DiseaseCrossReference;
import org.uniprot.core.cv.disease.DiseaseEntry;
import org.uniprot.core.cv.disease.builder.DiseaseCrossReferenceBuilder;
import org.uniprot.core.cv.disease.builder.DiseaseEntryBuilder;
import org.uniprot.core.cv.keyword.KeywordId;
import org.uniprot.core.cv.keyword.builder.KeywordEntryKeywordBuilder;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {DataStoreTestConfig.class, SupportDataApplication.class})
@WebMvcTest(DiseaseController.class)
class DiseaseControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private DiseaseService diseaseService;

    @Test
    void testGetDiseaseByAccession() throws Exception {
        String accession = "DI-12345";
        DiseaseEntry disease = createDisease();
        Mockito.when(this.diseaseService.findByUniqueId(accession)).thenReturn(disease);

        ResultActions response =
                this.mockMvc.perform(
                        MockMvcRequestBuilders.get("/disease/" + accession)
                                .param("accessionId", accession));

        response.andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath("$.id", equalTo(disease.getId())))
                .andExpect(jsonPath("$.accession", equalTo(disease.getAccession())))
                .andExpect(jsonPath("$.acronym", equalTo(disease.getAcronym())))
                .andExpect(jsonPath("$.definition", equalTo(disease.getDefinition())))
                .andExpect(
                        jsonPath(
                                "$.alternativeNames.size()",
                                equalTo(disease.getAlternativeNames().size())))
                .andExpect(jsonPath("$.keywords.size()", equalTo(disease.getKeywords().size())))
                .andExpect(
                        jsonPath(
                                "$.crossReferences.size()",
                                equalTo(disease.getCrossReferences().size())))
                .andExpect(
                        jsonPath(
                                "$.reviewedProteinCount",
                                equalTo(
                                        Integer.valueOf(
                                                disease.getReviewedProteinCount().toString()))))
                .andExpect(
                        jsonPath(
                                "$.unreviewedProteinCount",
                                equalTo(
                                        Integer.valueOf(
                                                disease.getUnreviewedProteinCount().toString()))));
    }

    @Test
    void invalidAccessionFormat() throws Exception {
        // Wrong format other than DI-xxxxx
        String accession = "RANDOM";

        // when
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get("/disease/" + accession)
                                .param("accessionId", accession));

        // then
        response.andDo(MockMvcResultHandlers.print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_TYPE,
                                        org.springframework.http.MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages[0]",
                                equalTo(
                                        "The disease id value has invalid format. It should match the regular expression 'DI-[0-9]{5}'")));
    }

    @Test
    void nonExistingAccession() throws Exception {
        String accession = "DI-00000";
        Mockito.when(this.diseaseService.findByUniqueId(accession))
                .thenThrow(new ResourceNotFoundException("{search.not.found}"));
        // when
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get("/disease/" + accession)
                                .param("accessionId", accession));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_TYPE,
                                        org.springframework.http.MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.*", contains("Resource not found")));
    }

    private DiseaseEntry createDisease() {
        String id = "Sample ID";
        String accession = "DI-12345";
        String acronym = "ACR";
        String definition = "test definition";
        List<String> alternativeNames = Arrays.asList("name1", "name2", "name3");

        DiseaseCrossReference xr1 =
                new DiseaseCrossReferenceBuilder()
                        .databaseType("DT1")
                        .id("ID1")
                        .propertiesSet(Arrays.asList("p1", "p2"))
                        .build();
        DiseaseCrossReference xr2 =
                new DiseaseCrossReferenceBuilder()
                        .databaseType("DT2")
                        .id("ID2")
                        .propertiesSet(Arrays.asList("p3", "p4"))
                        .build();
        List<DiseaseCrossReference> xrefs = Arrays.asList(xr1, xr2);
        List<KeywordId> keywords =
                Arrays.asList(
                        new KeywordEntryKeywordBuilder().id("keyword1").accession("kw-1").build(),
                        new KeywordEntryKeywordBuilder().id("keyword2").accession("kw-2").build());

        Long reviewedProteinCount = 10L;
        Long unreviewedProteinCount = 20L;

        DiseaseEntryBuilder builder = new DiseaseEntryBuilder();
        builder.id(id)
                .accession(accession)
                .acronym(acronym)
                .definition(definition)
                .alternativeNamesSet(alternativeNames);
        builder.crossReferencesSet(xrefs).keywordsSet(keywords);
        builder.reviewedProteinCount(reviewedProteinCount)
                .unreviewedProteinCount(unreviewedProteinCount);

        return builder.build();
    }
}
