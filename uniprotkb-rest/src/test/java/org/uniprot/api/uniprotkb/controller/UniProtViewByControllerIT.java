package org.uniprot.api.uniprotkb.controller;

import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.uniprot.api.AsyncDownloadMocks;
import org.uniprot.api.uniprotkb.view.ViewBy;
import org.uniprot.api.uniprotkb.view.service.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = UniProtViewByController.class)
@ContextConfiguration(classes = {AsyncDownloadMocks.class})
@AutoConfigureWebClient
class UniProtViewByControllerIT {
    @Autowired private MockMvc mockMvc;

    @MockBean private UniProtViewByECService ecService;

    @MockBean private UniProtViewByGoService goService;

    @MockBean private UniProtViewByKeywordService kwService;

    @MockBean private UniProtViewByPathwayService pwService;

    @MockBean private UniProtViewByTaxonomyService taxonService;

    @Test
    void testGetEC() throws Exception {
        mockEcService();

        String query = "organism_id:9606";
        String parent = "1.1.-.-";

        mockMvc.perform(get("/uniprotkb/view/ec").param("query", query).param("parent", parent))
                .andDo(log())
                .andExpect(jsonPath("$[0].id", is("1.1.1.-")))
                .andExpect(jsonPath("$[0].label", is("With NAD(+) or NADP(+) as acceptor")))
                .andExpect(jsonPath("$[1].id", is("1.1.3.-")))
                .andExpect(jsonPath("$[1].label", is("With oxygen as acceptor")));
    }

    private void mockEcService() {
        List<ViewBy> viewBys = new ArrayList<>();
        viewBys.add(
                MockServiceHelper.createViewBy(
                        "1.1.1.-",
                        "With NAD(+) or NADP(+) as acceptor",
                        346L,
                        UniProtViewByECService.URL_PREFIX + "1.1.1.-",
                        true));
        viewBys.add(
                MockServiceHelper.createViewBy(
                        "1.1.3.-",
                        "With oxygen as acceptor",
                        1L,
                        UniProtViewByECService.URL_PREFIX + "1.1.3.-",
                        true));

        when(ecService.get(anyString(), anyString())).thenReturn(viewBys);
    }

    @Test
    void testGetKeyword() throws Exception {
        mockKeywordService();
        String query = "organism_id:9606";
        String parent = "KW-0123";

        mockMvc.perform(
                        get("/uniprotkb/view/keyword")
                                .param("query", query)
                                .param("parent", parent))
                .andDo(log())
                .andExpect(jsonPath("$[0].id", is("KW-0128")))
                .andExpect(jsonPath("$[0].label", is("Catecholamine metabolism")))
                .andExpect(jsonPath("$[1].id", is("KW-0131")))
                .andExpect(jsonPath("$[1].label", is("Cell cycle")));
    }

    private void mockKeywordService() {
        List<ViewBy> viewBys = new ArrayList<>();
        viewBys.add(
                MockServiceHelper.createViewBy(
                        "KW-0128",
                        "Catecholamine metabolism",
                        5L,
                        UniProtViewByKeywordService.URL_PREFIX + "KW-0128",
                        false));
        viewBys.add(
                MockServiceHelper.createViewBy(
                        "KW-0131",
                        "Cell cycle",
                        102L,
                        UniProtViewByKeywordService.URL_PREFIX + "KW-0131",
                        true));

        when(kwService.get(anyString(), anyString())).thenReturn(viewBys);
    }

    @Test
    void testGetPathway() throws Exception {
        mockPathwayService();
        String query = "organism_id:9606";
        String parent = "3";

        mockMvc.perform(
                        get("/uniprotkb/view/pathway")
                                .param("query", query)
                                .param("parent", parent))
                .andDo(log())
                .andExpect(jsonPath("$[0].id", is("289")))
                .andExpect(jsonPath("$[0].label", is("Amine and polyamine biosynthesis")))
                .andExpect(jsonPath("$[1].id", is("456")))
                .andExpect(jsonPath("$[1].label", is("Amine and polyamine degradation")));
    }

    private void mockPathwayService() {
        List<ViewBy> viewBys = new ArrayList<>();
        viewBys.add(
                MockServiceHelper.createViewBy(
                        "289", "Amine and polyamine biosynthesis", 36L, null, false));
        viewBys.add(
                MockServiceHelper.createViewBy(
                        "456", "Amine and polyamine degradation", 1L, null, false));

        when(pwService.get(anyString(), anyString())).thenReturn(viewBys);
    }

    @Test
    void testGetGo() throws Exception {
        mockGoService();
        String query = "organism_id:9606";
        String parent = "GO:0002150";

        mockMvc.perform(get("/uniprotkb/view/go").param("query", query).param("parent", parent))
                .andDo(log())
                .andExpect(jsonPath("$[0].id", is("GO:0008150")))
                .andExpect(jsonPath("$[0].label", is("biological_process")))
                .andExpect(jsonPath("$[1].id", is("GO:0005575")))
                .andExpect(jsonPath("$[1].label", is("cellular_component")));
    }

    private void mockGoService() {
        List<ViewBy> viewBys = new ArrayList<>();
        viewBys.add(
                MockServiceHelper.createViewBy(
                        "GO:0008150",
                        "biological_process",
                        78L,
                        UniProtViewByGoService.URL_PREFIX + "GO:0008150",
                        true));
        viewBys.add(
                MockServiceHelper.createViewBy(
                        "GO:0005575",
                        "cellular_component",
                        70L,
                        UniProtViewByGoService.URL_PREFIX + "GO:0005575",
                        true));

        when(goService.get(anyString(), anyString())).thenReturn(viewBys);
    }

    @Test
    void testGetTaxonomy() throws Exception {
        mockTaxonService();
        String query = "organism_id:9606";
        String parent = "9605";

        mockMvc.perform(
                        get("/uniprotkb/view/taxonomy")
                                .param("query", query)
                                .param("parent", parent))
                .andDo(log())
                .andExpect(jsonPath("$[0].id", is("1425170")))
                .andExpect(jsonPath("$[0].label", is("Homo heidelbergensis")))
                .andExpect(jsonPath("$[1].id", is("9606")))
                .andExpect(jsonPath("$[1].label", is("Homo sapiens")));
    }

    private void mockTaxonService() {
        List<ViewBy> viewBys = new ArrayList<>();
        viewBys.add(
                MockServiceHelper.createViewBy(
                        "1425170",
                        "Homo heidelbergensis",
                        23L,
                        UniProtViewByTaxonomyService.URL_PREFIX + "1425170",
                        false));
        viewBys.add(
                MockServiceHelper.createViewBy(
                        "9606",
                        "Homo sapiens",
                        50L,
                        UniProtViewByTaxonomyService.URL_PREFIX + "9606",
                        false));

        when(taxonService.get(anyString(), anyString())).thenReturn(viewBys);
    }
}
