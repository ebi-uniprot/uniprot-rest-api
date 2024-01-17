package org.uniprot.api.support.data.crossref.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.uniprot.api.support.data.DataStoreTestConfig;
import org.uniprot.api.support.data.SupportDataRestApplication;
import org.uniprot.api.support.data.crossref.service.CrossRefService;
import org.uniprot.core.Statistics;
import org.uniprot.core.cv.xdb.CrossRefEntry;
import org.uniprot.core.cv.xdb.impl.CrossRefEntryBuilder;
import org.uniprot.core.impl.StatisticsBuilder;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {DataStoreTestConfig.class, SupportDataRestApplication.class})
@WebMvcTest(CrossRefController.class)
class CrossRefControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private CrossRefService crossRefService;

    @Test
    void testGetCrossRefByAccession() throws Exception {
        String accession = "DB-1234";
        CrossRefEntry crossRef = createDBXRef();
        Mockito.when(this.crossRefService.findByUniqueId(accession)).thenReturn(crossRef);

        ResultActions response =
                this.mockMvc.perform(
                        MockMvcRequestBuilders.get("/database/" + accession)
                                .param("accessionId", accession)
                                .header(HttpHeaders.ACCEPT, APPLICATION_JSON_VALUE));

        response.andDo(MockMvcResultHandlers.log())
                .andExpect(jsonPath("$.id", equalTo(crossRef.getId())))
                .andExpect(jsonPath("$.abbrev", equalTo(crossRef.getAbbrev())))
                .andExpect(jsonPath("$.name", equalTo(crossRef.getName())))
                .andExpect(jsonPath("$.pubMedId", equalTo(crossRef.getPubMedId())))
                .andExpect(jsonPath("$.doiId", equalTo(crossRef.getDoiId())))
                .andExpect(jsonPath("$.linkType", equalTo(crossRef.getLinkType())))
                .andExpect(jsonPath("$.servers", equalTo(crossRef.getServers())))
                .andExpect(jsonPath("$.dbUrl", equalTo(crossRef.getDbUrl())))
                .andExpect(jsonPath("$.category", equalTo(crossRef.getCategory())))
                .andExpect(
                        jsonPath(
                                "$.statistics.reviewedProteinCount",
                                equalTo(
                                        Long.valueOf(
                                                        crossRef.getStatistics()
                                                                .getReviewedProteinCount())
                                                .intValue())))
                .andExpect(
                        jsonPath(
                                "$.statistics.unreviewedProteinCount",
                                equalTo(
                                        Long.valueOf(
                                                        crossRef.getStatistics()
                                                                .getUnreviewedProteinCount())
                                                .intValue())));
    }

    private CrossRefEntry createDBXRef() {
        String random = UUID.randomUUID().toString();
        String ac = random + "-AC-";
        String ab = random + "-AB-";
        String nm = random + "-NM-";
        String pb = random + "-PB-";
        String di = random + "-DI-";
        String lt = random + "-LT-";
        String sr = random + "-SR-";
        String du = random + "-DU-";
        String ct = random + "-CT-";

        Statistics statistics =
                new StatisticsBuilder().reviewedProteinCount(2L).unreviewedProteinCount(3L).build();

        CrossRefEntryBuilder builder = new CrossRefEntryBuilder();
        builder.abbrev(ab).id(ac).category(ct).dbUrl(du);
        builder.doiId(di).linkType(lt).name(nm).pubMedId(pb).serversAdd(sr);
        builder.statistics(statistics);
        return builder.build();
    }
}
