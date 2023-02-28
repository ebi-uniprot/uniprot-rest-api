package org.uniprot.api.statistics.controller;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
@Testcontainers
class StatisticControllerIT {
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
    public void getByReleaseAndType() throws Exception {
        this.mockMvc
                .perform(get("/statistics/releases/2021_03/reviewed"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .json(
                                        "{\"results\":["
                                                + "{\"name\":\"SEQUENCE_AMINO_ACID\",\"totalCount\":329,\"totalEntryCount\":254,\"attributes\":[{\"name\":\"AMINO_ACID_U\",\"count\":329,\"entryCount\":254,\"description\":null,\"statisticType\":\"REVIEWED\"}]},"
                                                + "{\"name\":\"TOP_ORGANISM\",\"totalCount\":716,\"totalEntryCount\":716,\"attributes\":[{\"name\":\"Salmonella paratyphi B (strain ATCC BAA-1250 / SPB7)\",\"count\":716,\"entryCount\":716,\"description\":null,\"statisticType\":\"REVIEWED\"}]},"
                                                + "{\"name\":\"EUKARYOTA\",\"totalCount\":44817,\"totalEntryCount\":44817,\"attributes\":[{\"name\":\"Fungi\",\"count\":35360,\"entryCount\":35360,\"description\":null,\"statisticType\":\"REVIEWED\"},{\"name\":\"Insecta\",\"count\":9457,\"entryCount\":9457,\"description\":null,\"statisticType\":\"REVIEWED\"}]}"
                                                + "]}"));
    }

    @Test
    public void getByReleaseAndTypeAndSingleCategory() throws Exception {
        this.mockMvc
                .perform(get("/statistics/releases/2021_03/reviewed?categories=EUKARYOTA"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .json(
                                        "{\"results\":["
                                                + "{\"name\":\"EUKARYOTA\",\"totalCount\":44817,\"totalEntryCount\":44817,\"attributes\":[{\"name\":\"Fungi\",\"count\":35360,\"entryCount\":35360,\"description\":null,\"statisticType\":\"REVIEWED\"},{\"name\":\"Insecta\",\"count\":9457,\"entryCount\":9457,\"description\":null,\"statisticType\":\"REVIEWED\"}]}"
                                                + "]}"));
    }

    @Test
    public void getByReleaseAndTypeAndMultipleCategories() throws Exception {
        this.mockMvc
                .perform(
                        get(
                                "/statistics/releases/2021_03/reviewed?categories=EUKARYOTA,TOP_ORGANISM"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .json(
                                        "{\"results\":["
                                                + "{\"name\":\"TOP_ORGANISM\",\"totalCount\":716,\"totalEntryCount\":716,\"attributes\":[{\"name\":\"Salmonella paratyphi B (strain ATCC BAA-1250 / SPB7)\",\"count\":716,\"entryCount\":716,\"description\":null,\"statisticType\":\"REVIEWED\"}]},"
                                                + "{\"name\":\"EUKARYOTA\",\"totalCount\":44817,\"totalEntryCount\":44817,\"attributes\":[{\"name\":\"Fungi\",\"count\":35360,\"entryCount\":35360,\"description\":null,\"statisticType\":\"REVIEWED\"},{\"name\":\"Insecta\",\"count\":9457,\"entryCount\":9457,\"description\":null,\"statisticType\":\"REVIEWED\"}]}"
                                                + "]}"));
    }

    @Test
    void getByReleaseAndTypeAndCategories_whenWrongStatisticTypeIsSpecified_returnsABadRequest()
            throws Exception {
        this.mockMvc
                .perform(
                        get("/statistics/releases/2021_03/wrongStatType?categories=EUKARYOTA,TOP_ORGANISM")
                                .header(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsStringIgnoringCase("Statistic Type")));
    }

    @Test
    void getByReleaseAndTypeAndCategories_whenWrongAWrongCategoryIsSpecified_returnsABadRequest()
            throws Exception {
        this.mockMvc
                .perform(
                        get("/statistics/releases/2021_03/reviewed?categories=EUKARYOTA,wrongCategory")
                                .header(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsStringIgnoringCase("Statistic Category")));
    }
}
