package org.uniprot.api.support.data.statistics.controller;

import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@AutoConfigureMockMvc
@SpringBootTest
@Testcontainers
class StatisticsControllerIT {
    private static final String POSTGRES_IMAGE_VERSION = "postgres:11.1";
    @Autowired private MockMvc mockMvc;

    @Container
    private static final PostgreSQLContainer<?> postgreSQL =
            new PostgreSQLContainer<>(DockerImageName.parse(POSTGRES_IMAGE_VERSION))
                    .withInitScript("init.sql");

    @DynamicPropertySource
    static void postgreSQLProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQL::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQL::getUsername);
        registry.add("spring.datasource.password", postgreSQL::getPassword);
    }

    @Test
    void getByReleaseAndType() throws Exception {
        this.mockMvc
                .perform(get("/statistics/releases/2021_03/reviewed"))
                .andDo(log())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results.size()", is(3)))
                .andExpect(jsonPath("$.results[0].categoryName", is("SEQUENCE_AMINO_ACID")))
                .andExpect(jsonPath("$.results[0].searchField", is("sf Sequence Amino Acid")))
                .andExpect(jsonPath("$.results[0].label", is("Sequence Amino Acid")))
                .andExpect(jsonPath("$.results[0].totalCount", is(329)))
                .andExpect(jsonPath("$.results[0].items.size()", is(1)))
                .andExpect(jsonPath("$.results[0].items[0].name", is("AMINO_ACID_U")))
                .andExpect(jsonPath("$.results[0].items[0].count", is(329)))
                .andExpect(jsonPath("$.results[0].items[0].entryCount", is(254)))
                .andExpect(jsonPath("$.results[1].categoryName", is("TOP_ORGANISM")))
                .andExpect(jsonPath("$.results[1].searchField", is("sf Organism")))
                .andExpect(jsonPath("$.results[1].label", is("Top Organism")))
                .andExpect(jsonPath("$.results[1].totalCount", is(716)))
                .andExpect(jsonPath("$.results[1].items.size()", is(1)))
                .andExpect(
                        jsonPath(
                                "$.results[1].items[0].name",
                                is("Salmonella paratyphi B (strain ATCC BAA-1250 / SPB7)")))
                .andExpect(jsonPath("$.results[1].items[0].count", is(716)))
                .andExpect(jsonPath("$.results[1].items[0].entryCount", is(716)))
                .andExpect(jsonPath("$.results[2].categoryName", is("EUKARYOTA")))
                .andExpect(jsonPath("$.results[2].searchField", is("sf Eukaryota")))
                .andExpect(jsonPath("$.results[2].label", is("Eukaryota")))
                .andExpect(jsonPath("$.results[2].totalCount", is(44817)))
                .andExpect(jsonPath("$.results[2].items.size()", is(2)))
                .andExpect(jsonPath("$.results[2].items[0].name", is("Fungi")))
                .andExpect(jsonPath("$.results[2].items[0].count", is(35360)))
                .andExpect(jsonPath("$.results[2].items[0].entryCount", is(35360)))
                .andExpect(jsonPath("$.results[2].items[1].name", is("Insecta")))
                .andExpect(jsonPath("$.results[2].items[1].count", is(9457)))
                .andExpect(jsonPath("$.results[2].items[1].entryCount", is(9457)));
    }

    @Test
    void getByReleaseAndTypeAndSingleCategory() throws Exception {
        this.mockMvc
                .perform(get("/statistics/releases/2021_03/reviewed?categories=EUKARYOTA"))
                .andDo(log())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.results[0].categoryName", is("EUKARYOTA")))
                .andExpect(jsonPath("$.results[0].searchField", is("sf Eukaryota")))
                .andExpect(jsonPath("$.results[0].label", is("Eukaryota")))
                .andExpect(jsonPath("$.results[0].totalCount", is(44817)))
                .andExpect(jsonPath("$.results[0].items.size()", is(2)))
                .andExpect(jsonPath("$.results[0].items[0].name", is("Fungi")))
                .andExpect(jsonPath("$.results[0].items[0].count", is(35360)))
                .andExpect(jsonPath("$.results[0].items[0].entryCount", is(35360)))
                .andExpect(jsonPath("$.results[0].items[1].name", is("Insecta")))
                .andExpect(jsonPath("$.results[0].items[1].count", is(9457)))
                .andExpect(jsonPath("$.results[0].items[1].entryCount", is(9457)));
    }

    @Test
    void getByReleaseAndTypeAndMultipleCategories() throws Exception {
        this.mockMvc
                .perform(
                        get(
                                "/statistics/releases/2021_03/reviewed?categories=EUKARYOTA,TOP_ORGANISM"))
                .andDo(log())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results.size()", is(2)))
                .andExpect(jsonPath("$.results[0].categoryName", is("TOP_ORGANISM")))
                .andExpect(jsonPath("$.results[0].searchField", is("sf Organism")))
                .andExpect(jsonPath("$.results[0].label", is("Top Organism")))
                .andExpect(jsonPath("$.results[0].totalCount", is(716)))
                .andExpect(jsonPath("$.results[0].items.size()", is(1)))
                .andExpect(
                        jsonPath(
                                "$.results[0].items[0].name",
                                is("Salmonella paratyphi B (strain ATCC BAA-1250 / SPB7)")))
                .andExpect(jsonPath("$.results[0].items[0].count", is(716)))
                .andExpect(jsonPath("$.results[0].items[0].entryCount", is(716)))
                .andExpect(jsonPath("$.results[1].categoryName", is("EUKARYOTA")))
                .andExpect(jsonPath("$.results[1].searchField", is("sf Eukaryota")))
                .andExpect(jsonPath("$.results[1].label", is("Eukaryota")))
                .andExpect(jsonPath("$.results[1].totalCount", is(44817)))
                .andExpect(jsonPath("$.results[1].items.size()", is(2)))
                .andExpect(jsonPath("$.results[1].items[0].name", is("Fungi")))
                .andExpect(jsonPath("$.results[1].items[0].count", is(35360)))
                .andExpect(jsonPath("$.results[1].items[0].entryCount", is(35360)))
                .andExpect(jsonPath("$.results[1].items[1].name", is("Insecta")))
                .andExpect(jsonPath("$.results[1].items[1].count", is(9457)))
                .andExpect(jsonPath("$.results[1].items[1].entryCount", is(9457)));
    }

    @Test
    void getByReleaseAndTypeAndCategories_whenWrongStatisticTypeIsSpecified_returnsABadRequest()
            throws Exception {
        this.mockMvc
                .perform(
                        get(
                                "/statistics/releases/2021_03/wrongStatType?categories=EUKARYOTA,TOP_ORGANISM"))
                .andDo(log())
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsStringIgnoringCase("Statistic Type")));
    }

    @Test
    void getByReleaseAndTypeAndCategories_whenWrongAWrongCategoryIsSpecified_returnsABadRequest()
            throws Exception {
        this.mockMvc
                .perform(
                        get(
                                "/statistics/releases/2021_03/reviewed?categories=EUKARYOTA,wrongCategory"))
                .andDo(log())
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsStringIgnoringCase("Statistic Category")));
    }
}
