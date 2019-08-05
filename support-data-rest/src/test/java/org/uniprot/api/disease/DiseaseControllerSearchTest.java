package org.uniprot.api.disease;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.hamcrest.Matchers;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.uniprot.api.disease.DiseaseController;
import org.uniprot.api.repository.SolrTestConfig;
import org.uniprot.api.support_data.SupportDataApplication;
import org.uniprot.core.builder.DiseaseBuilder;
import org.uniprot.core.cv.disease.Disease;
import org.uniprot.core.cv.impl.DiseaseFileReader;
import org.uniprot.core.json.parser.disease.DiseaseJsonConfig;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.disease.DiseaseDocument;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {SolrTestConfig.class, SupportDataApplication.class})
@WebMvcTest(DiseaseController.class)
public class DiseaseControllerSearchTest {
    // accession
    private ObjectMapper diseaseObjectMapper = DiseaseJsonConfig.getInstance().getFullObjectMapper();

    @Autowired
    private SolrTemplate solrTemplate;

    @Autowired
    private MockMvc mockMvc;
    private static boolean isIndexed = false;

    @BeforeEach
    void setUp() throws IOException, SolrServerException {
        if (!isIndexed) {
            DiseaseFileReader diseaseFileReader = new DiseaseFileReader();
            List<Disease> diseases = diseaseFileReader.parse("src/test/resources/sample-humdisease.txt");
            // convert the disease to disease document
            List<DiseaseDocument> docs = convertToDocs(diseases);
            this.solrTemplate.getSolrClient().addBeans(SolrCollection.disease.name(), docs);
            this.solrTemplate.getSolrClient().commit(SolrCollection.disease.name());
            isIndexed = true;
        }
    }

    @Test
    @DisplayName("Search disease with the keyword in its ID")
    void testSearchDiseaseWithIdMatch() throws Exception {
        // given
        String searchString = "reductase";

        // when
        ResultActions response = mockMvc.perform(
                MockMvcRequestBuilders.get("/disease/search/")
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                        .param("query", searchString));

        // then
        JSONObject result = new JSONObject(response.andReturn().getResponse().getContentAsString());
        JSONArray results = result.getJSONArray("results");
        Assertions.assertEquals(1, results.length());

        // then
        response.andDo(MockMvcResultHandlers.print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results[0].id", Matchers.containsString(searchString)));

    }

    // search by acronym
    @Test
    @DisplayName("Search disease with the keyword in its ACRONYM")
    void testSearchDiseaseWithAcronymMatch() throws Exception {
        // given
        String searchString = "AMOXAD";

        // when
        ResultActions response = mockMvc.perform(
                MockMvcRequestBuilders.get("/disease/search/")
                        .param("query", searchString)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                        );

        // then
        JSONObject result = new JSONObject(response.andReturn().getResponse().getContentAsString());
        JSONArray results = result.getJSONArray("results");
        Assertions.assertEquals(1, results.length());

        // then
        response.andDo(MockMvcResultHandlers.print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results[0].acronym", Matchers.equalTo(searchString)));

    }


    @Test
    @DisplayName("Search disease with the keyword in its definition")
    void testSearchDiseaseWithDefinitionMatch() throws Exception {
        // given
        String searchString = "and in some cases sudden death";

        // when
        ResultActions response = mockMvc.perform(
                MockMvcRequestBuilders.get("/disease/search/")
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                        .param("query", searchString));

        // then
        JSONObject result = new JSONObject(response.andReturn().getResponse().getContentAsString());
        JSONArray results = result.getJSONArray("results");
        Assertions.assertEquals(1, results.length());

        // then
        response.andDo(MockMvcResultHandlers.print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results[0].definition", Matchers.containsString(searchString)));

    }

    // all the fields which are there in content (id, acronym, definition, synonyms, keywords, accession)

    @Test
    @DisplayName("Search disease with the keyword in its synonym")
    void testSearchDiseaseWithSynonymMatch() throws Exception {
        // given
        String searchString = "SCHAD deficiency";

        // when
        ResultActions response = mockMvc.perform(
                MockMvcRequestBuilders.get("/disease/search/")
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                        .param("query", searchString));

        // then
        JSONObject result = new JSONObject(response.andReturn().getResponse().getContentAsString());
        JSONArray results = result.getJSONArray("results");
        Assertions.assertEquals(1, results.length());

        // then
        response.andDo(MockMvcResultHandlers.print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results[0].alternativeNames[2]", Matchers.equalTo(searchString)));

    }

    // search by KW
    @Test
    @DisplayName("Search disease with the keyword in its KW")
    void testSearchDiseaseWithKWMatch() throws Exception {
        // given
        String searchString = "Cataract";

        // when
        ResultActions response = mockMvc.perform(
                MockMvcRequestBuilders.get("/disease/search/")
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                        .param("query", searchString));

        // then
        JSONObject result = new JSONObject(response.andReturn().getResponse().getContentAsString());
        JSONArray results = result.getJSONArray("results");
        Assertions.assertEquals(1, results.length());

        // then
        response.andDo(MockMvcResultHandlers.print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results[0].keywords[1].id", Matchers.equalTo(searchString)));

    }

    // search by accession
    @Test
    @DisplayName("Search disease with the keyword in its accession")
    void testSearchDiseaseWithAccessionMatch() throws Exception {
        // given
        String searchString = "DI-03495";

        // when
        ResultActions response = mockMvc.perform(
                MockMvcRequestBuilders.get("/disease/search/")
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                        .param("query", searchString));

        // then
        JSONObject result = new JSONObject(response.andReturn().getResponse().getContentAsString());
        JSONArray results = result.getJSONArray("results");
        Assertions.assertEquals(1, results.length());

        // then
        response.andDo(MockMvcResultHandlers.print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results[0].accession", Matchers.equalTo(searchString)));

    }
    // search by cross reference
    @Test
    @DisplayName("Search disease with the keyword in its KW")
    void testSearchDiseaseByCrossRef() throws Exception {
        // given
        String searchString = "D020167";

        // when
        ResultActions response = mockMvc.perform(
                MockMvcRequestBuilders.get("/disease/search/")
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                        .param("query", searchString));

        // then
        JSONObject result = new JSONObject(response.andReturn().getResponse().getContentAsString());
        JSONArray results = result.getJSONArray("results");
        Assertions.assertEquals(0, results.length());
    }


    //2. search by name = id, acronym, definition, synonyms, keywords
    @Test
    @DisplayName("Search disease by name with the keyword in its ID")
    void testSearchDiseaseByNameWithIDMatch() throws Exception {
        // given
        String searchString = "reductase";

        // when
        ResultActions response = mockMvc.perform(
                MockMvcRequestBuilders.get("/disease/search/")
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                        .param("query", "name:" + searchString));

        // then
        JSONObject result = new JSONObject(response.andReturn().getResponse().getContentAsString());
        JSONArray results = result.getJSONArray("results");
        Assertions.assertEquals(1, results.length());

        // then
        response.andDo(MockMvcResultHandlers.print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results[0].id", Matchers.containsString(searchString)));

    }

    @Test
    @DisplayName("Search disease by name with the keyword in its acronym")
    void testSearchDiseaseByNameWithAcronymMatch() throws Exception {
        // given
        String searchString = "AMOXAD";

        // when
        ResultActions response = mockMvc.perform(
                MockMvcRequestBuilders.get("/disease/search/")
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                        .param("query", "name:" + searchString));

        // then
        JSONObject result = new JSONObject(response.andReturn().getResponse().getContentAsString());
        JSONArray results = result.getJSONArray("results");
        Assertions.assertEquals(1, results.length());

        // then
        response.andDo(MockMvcResultHandlers.print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results[0].acronym", Matchers.equalTo(searchString)));

    }

    @Test
    @DisplayName("Search disease by name with the keyword in its definition")
    void testSearchDiseaseByNameWithDefMatch() throws Exception {
        // given
        String searchString = "and in some cases sudden death";

        // when
        ResultActions response = mockMvc.perform(
                MockMvcRequestBuilders.get("/disease/search/")
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                        .param("query", "name:" + searchString));

        // then
        JSONObject result = new JSONObject(response.andReturn().getResponse().getContentAsString());
        JSONArray results = result.getJSONArray("results");
        Assertions.assertEquals(1, results.length());

        // then
        response.andDo(MockMvcResultHandlers.print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results[0].definition", Matchers.containsString(searchString)));

    }

    @Test
    @DisplayName("Search disease by name with the keyword in its synonym")
    void testSearchDiseaseByNameWithSynonymMatch() throws Exception {
        // given
        String searchString = "HAD deficiency";

        // when
        ResultActions response = mockMvc.perform(
                MockMvcRequestBuilders.get("/disease/search/")
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                        .param("query", "name:" + searchString));

        // then
        JSONObject result = new JSONObject(response.andReturn().getResponse().getContentAsString());
        JSONArray results = result.getJSONArray("results");
        Assertions.assertEquals(1, results.length());

        // then
        response.andDo(MockMvcResultHandlers.print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results[0].alternativeNames[0]", Matchers.containsString(searchString)));

    }

    @Test
    @DisplayName("Search disease by name with the keyword in its keywords")
    void testSearchDiseaseByNameWithKWMatch() throws Exception {
        // given
        String searchString = "Cataract";

        // when
        ResultActions response = mockMvc.perform(
                MockMvcRequestBuilders.get("/disease/search/")
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                        .param("query", "name:" + searchString));

        // then
        JSONObject result = new JSONObject(response.andReturn().getResponse().getContentAsString());
        JSONArray results = result.getJSONArray("results");
        Assertions.assertEquals(1, results.length());

        // then
        response.andDo(MockMvcResultHandlers.print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results[0].keywords[1].id", Matchers.containsString(searchString)));

    }

    // search by accession
    @Test
    @DisplayName("Try to search disease by name with accession as search string")
    void testSearchDiseaseWithAccession() throws Exception {
        // given
        String searchString = "DI-03495";

        // when
        ResultActions response = mockMvc.perform(
                MockMvcRequestBuilders.get("/disease/search/")
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                        .param("query", "name:" + searchString));

        // then
        JSONObject result = new JSONObject(response.andReturn().getResponse().getContentAsString());
        JSONArray results = result.getJSONArray("results");
        Assertions.assertEquals(0, results.length());
    }

    // search by accession
    @Test
    @DisplayName("Try to search disease by acronym")
    void testSearchDiseaseByAcronym() throws Exception {
        // given
        String searchString = "DI-03495";

        // when
        ResultActions response = mockMvc.perform(
                MockMvcRequestBuilders.get("/disease/search/")
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                        .param("query", "acronym:" + searchString));

        // then
        response.andDo(MockMvcResultHandlers.print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.messages[0]", Matchers.equalTo("'acronym' is not a valid search field")));
    }

    @Test
    void testSearchOneDisease() throws Exception {
        // given
        String accession = "DI-04240";

        // when
        ResultActions response = mockMvc.perform(
                MockMvcRequestBuilders.get("/disease/search/")
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                        .param("query", "accession:" + accession));

        // then
        response.andDo(MockMvcResultHandlers.print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.*.accession", Matchers.contains(accession)));
    }

    private List<DiseaseDocument> convertToDocs(List<Disease> diseases) {
        List<DiseaseDocument> docs = new ArrayList<>();

        for (Disease disease : diseases) {
            List<String> kwIds = new ArrayList<>();
            if (disease.getKeywords() != null) {
                kwIds = disease.getKeywords().stream().map(kw -> kw.getId()).collect(Collectors.toList());
            }
            // name is a combination of id, acronym, definition, synonyms, keywords
            List<String> name = new ArrayList<>();
            name.add(disease.getId());
            name.add(disease.getAcronym());
            name.add(disease.getDefinition());
            name.addAll(kwIds);
            name.add(StringUtils.join(disease.getAlternativeNames()));

            // content is name + accession
            List<String> content = new ArrayList<>();
            content.addAll(name);
            content.add(disease.getAccession());

            // create disease document
            DiseaseDocument.DiseaseDocumentBuilder builder = DiseaseDocument.builder();
            builder.accession(disease.getAccession());
            builder.name(name).content(content);
            byte[] diseaseByte = getDiseaseObjectBinary(disease);
            builder.diseaseObj(ByteBuffer.wrap(diseaseByte));

            DiseaseDocument doc = builder.build();
            docs.add(doc);
        }

        return docs;
    }


    private byte[] getDiseaseObjectBinary(Disease disease) {
        try {
            DiseaseBuilder diseaseBuilder = DiseaseBuilder.newInstance().from(disease);
            return this.diseaseObjectMapper.writeValueAsBytes(diseaseBuilder.build());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to parse disease to binary json: ", e);
        }
    }
}
