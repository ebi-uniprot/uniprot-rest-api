package uk.ac.ebi.uniprot.disease;

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
import uk.ac.ebi.uniprot.api.common.exception.ResourceNotFoundException;
import uk.ac.ebi.uniprot.api.disease.DiseaseController;
import uk.ac.ebi.uniprot.api.disease.DiseaseService;
import uk.ac.ebi.uniprot.api.support_data.SupportDataApplication;
import uk.ac.ebi.uniprot.cv.disease.CrossReference;
import uk.ac.ebi.uniprot.cv.disease.Disease;
import uk.ac.ebi.uniprot.cv.keyword.Keyword;
import uk.ac.ebi.uniprot.cv.keyword.impl.KeywordImpl;
import uk.ac.ebi.uniprot.domain.builder.DiseaseBuilder;
import uk.ac.ebi.uniprot.repository.SolrTestConfig;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes={SolrTestConfig.class, SupportDataApplication.class})
@WebMvcTest(DiseaseController.class)
class DiseaseControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private DiseaseService diseaseService;

    @Test
    void testGetDiseaseByAccession() throws Exception {
        String accession = "DI-12345";
        Disease disease = createDisease();
        Mockito.when(this.diseaseService.findByAccession(accession)).thenReturn(disease);

        ResultActions response = this.mockMvc.perform(
                MockMvcRequestBuilders
                        .get("/disease/" + accession)
                        .param("accessionId", accession)
        );

        response.andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath("$.id", equalTo(disease.getId())))
                .andExpect(jsonPath("$.accession", equalTo(disease.getAccession())))
                .andExpect(jsonPath("$.acronym", equalTo(disease.getAcronym())))
                .andExpect(jsonPath("$.definition", equalTo(disease.getDefinition())))
                .andExpect(jsonPath("$.alternativeNames.size()", equalTo(disease.getAlternativeNames().size())))
                .andExpect(jsonPath("$.keywords.size()", equalTo(disease.getKeywords().size())))
                .andExpect(jsonPath("$.crossReferences.size()", equalTo(disease.getCrossReferences().size())))
                .andExpect(jsonPath("$.reviewedProteinCount", equalTo(Integer.valueOf(disease.getReviewedProteinCount().toString()))))
                .andExpect(jsonPath("$.unreviewedProteinCount", equalTo(Integer.valueOf(disease.getUnreviewedProteinCount().toString()))));
    }

    @Test
    void invalidAccessionFormat() throws Exception {
        // Wrong format other than DI-xxxxx
        String accession = "RANDOM";

        // when
        ResultActions response = mockMvc.perform(MockMvcRequestBuilders.
                get("/disease/" + accession)
                .param("accessionId", accession));

        // then
        response.andDo(MockMvcResultHandlers.print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, org.springframework.http.MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages[0]",
                        equalTo("Invalid accession format. Expected DI-xxxxx")));
    }

    @Test
    void nonExistingAccession() throws Exception {
        String accession = "DI-00000";
        Mockito.when(this.diseaseService.findByAccession(accession)).thenThrow(new ResourceNotFoundException("{search.not.found}"));
        // when
        ResultActions response = mockMvc.perform(MockMvcRequestBuilders.
                get("/disease/" + accession)
                .param("accessionId", accession));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, org.springframework.http.MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.*",contains("Resource not found")));
    }

    private Disease createDisease(){
        String id = "Sample ID";
        String accession = "DI-12345";
        String acronym = "ACR";
        String definition = "test definition";
        List<String> alternativeNames = Arrays.asList("name1", "name2", "name3");

        CrossReference xr1 = new CrossReference("DT1", "ID1", Arrays.asList("p1", "p2"));
        CrossReference xr2 = new CrossReference("DT2", "ID2", Arrays.asList("p3", "p4"));
        List<CrossReference> xrefs = Arrays.asList(xr1, xr2);
        List<CrossReference> crossReferences = xrefs;
        List<Keyword> keywords = Arrays.asList(new KeywordImpl("keyword1", "kw-1"),
                new KeywordImpl("keyword2", "kw-2"));

        Long reviewedProteinCount = 10L;
        Long unreviewedProteinCount = 20L;

        DiseaseBuilder builder = DiseaseBuilder.newInstance();
        builder.id(id).accession(accession).acronym(acronym).definition(definition).alternativeNames(alternativeNames);
        builder.crossReferences(xrefs).keywords(keywords);
        builder.reviewedProteinCount(reviewedProteinCount).unreviewedProteinCount(unreviewedProteinCount);

        return builder.build();
    }
}
