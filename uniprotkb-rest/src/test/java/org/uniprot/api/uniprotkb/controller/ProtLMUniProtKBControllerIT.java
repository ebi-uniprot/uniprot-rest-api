package org.uniprot.api.uniprotkb.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.http.HttpHeaders;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.uniprot.api.uniprotkb.UniProtKBREST;
import org.uniprot.api.uniprotkb.common.repository.store.protlm.ProtLMStoreClient;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.xml.jaxb.uniprot.Uniprot;
import org.uniprot.core.xml.uniprot.GoogleUniProtEntryConverter;
import org.uniprot.store.datastore.voldemort.uniprot.VoldemortInMemoryUniprotEntryStore;
import org.uniprot.store.indexer.DataStoreManager;

@ContextConfiguration(classes = {UniProtKBREST.class})
@ActiveProfiles(profiles = "offline")
@AutoConfigureWebClient
@WebMvcTest(ProtLMUniProtKBController.class)
@ExtendWith(value = {SpringExtension.class, MockitoExtension.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ProtLMUniProtKBControllerIT {
    @RegisterExtension static DataStoreManager storeManager = new DataStoreManager();

    private ProtLMStoreClient storeClient;
    private String accessionId = "A0A1D5RGX0";

    @Autowired private MockMvc mockMvc;

    @BeforeAll
    void init() throws JAXBException {
        DataStoreManager dsm = storeManager;
        storeClient =
                new ProtLMStoreClient(
                        VoldemortInMemoryUniprotEntryStore.getInstance("avro-uniprot"));
        dsm.addStore(DataStoreManager.StoreType.GOOGLE_PROTLM, storeClient);

        UniProtKBEntry entry = createProtLMEntry();
        dsm.saveToStore(DataStoreManager.StoreType.GOOGLE_PROTLM, entry);
    }

    private UniProtKBEntry createProtLMEntry() throws JAXBException {
        String file = "/it/" + accessionId + ".xml";
        InputStream inputStream = ProtLMUniProtKBControllerIT.class.getResourceAsStream(file);

        JAXBContext jaxbContext = JAXBContext.newInstance("org.uniprot.core.xml.jaxb.uniprot");

        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        Uniprot xmlEntry = (Uniprot) jaxbUnmarshaller.unmarshal(inputStream);
        assertNotNull(xmlEntry);

        GoogleUniProtEntryConverter converter = new GoogleUniProtEntryConverter();
        List<UniProtKBEntry> uniProtEntries =
                xmlEntry.getEntry().stream().map(converter::fromXml).toList();
        assertNotNull(uniProtEntries);
        assertFalse(uniProtEntries.isEmpty());
        return uniProtEntries.get(0);
    }

    @Test
    void testGetProtLMEntry_success() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get("/uniprotkb/protlm/" + accessionId)
                                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.primaryAccession", Matchers.is(accessionId)));
    }

    @Test
    void testGetProtLMEntry_notFound() throws Exception {
        String unknownAccession = "P05067";

        // when
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get("/uniprotkb/protlm/" + unknownAccession)
                                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.NOT_FOUND.value()));
    }

    @Test
    void testGetProtLMEntry_invalidAccessionPattern() throws Exception {
        String invalidAccession = "!@#INVALID";

        // when
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get("/uniprotkb/protlm/" + invalidAccession)
                                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.BAD_REQUEST.value()));
    }

    @Test
    void testGetProtLMEntry_invalidFormat() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get("/uniprotkb/protlm/" + accessionId)
                                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.url")
                                .value("http://localhost/uniprotkb/protlm/" + accessionId))
                .andExpect(MockMvcResultMatchers.jsonPath("$.messages").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.messages.length()").value(1))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.messages[0]")
                                .value(
                                        "Invalid request received. Requested media type/format not accepted: 'application/xml'."));
    }

    @TestConfiguration
    static class LocalTestConfig {
        @Bean
        public ProtLMStoreClient protLMStoreClient() {
            return new ProtLMStoreClient(
                    VoldemortInMemoryUniprotEntryStore.getInstance("avro-uniprot"));
        }
    }
}
