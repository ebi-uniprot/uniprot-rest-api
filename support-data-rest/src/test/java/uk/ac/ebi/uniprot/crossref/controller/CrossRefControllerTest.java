package uk.ac.ebi.uniprot.crossref.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import uk.ac.ebi.uniprot.api.crossref.controller.CrossRefController;
import uk.ac.ebi.uniprot.api.crossref.model.CrossRef;
import uk.ac.ebi.uniprot.api.crossref.service.CrossRefService;
import uk.ac.ebi.uniprot.api.support_data.SupportDataApplication;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes=SupportDataApplication.class)
@WebMvcTest(CrossRefController.class)
public class CrossRefControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private CrossRefService crossRefService;

    @Test
    void testGetCrossRefByAccession() throws Exception {
        String accession = "DB-1234";
        CrossRef crossRef = createDBXRef();
        Mockito.when(this.crossRefService.findByAccession(accession)).thenReturn(crossRef);

        ResultActions response = this.mockMvc.perform(
                MockMvcRequestBuilders
                        .get("/v1/xref/accession/" + accession)
                        .param("accessionId", accession)
        );

        response.andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath("$.accession", equalTo(crossRef.getAccession())))
                .andExpect(jsonPath("$.abbrev", equalTo(crossRef.getAbbrev())))
                .andExpect(jsonPath("$.name", equalTo(crossRef.getName())))
                .andExpect(jsonPath("$.pubMedId", equalTo(crossRef.getPubMedId())))
                .andExpect(jsonPath("$.doiId", equalTo(crossRef.getDoiId())))
                .andExpect(jsonPath("$.linkType", equalTo(crossRef.getLinkType())))
                .andExpect(jsonPath("$.server", equalTo(crossRef.getServer())))
                .andExpect(jsonPath("$.dbUrl", equalTo(crossRef.getDbUrl())))
                .andExpect(jsonPath("$.category", equalTo(crossRef.getCategory())));
    }

    private CrossRef createDBXRef(){
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

        CrossRef.CrossRefBuilder builder = CrossRef.builder();
        builder.abbrev(ab).accession(ac).category(ct).dbUrl(du);
        builder.doiId(di).linkType(lt).name(nm).pubMedId(pb).server(sr);
        return builder.build();
    }
}
