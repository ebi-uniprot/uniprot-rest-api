package org.uniprot.api.idmapping.service.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.uniprot.api.idmapping.service.impl.PIRServiceImpl.HTTP_HEADERS;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.idmapping.controller.request.IdMappingJobRequest;
import org.uniprot.api.idmapping.model.IdMappingResult;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.store.config.idmapping.IdMappingFieldConfig;

class PIRServiceImplTest {
    private RestTemplate restTemplate;
    private PIRServiceImpl pirService;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        pirService = new PIRServiceImpl(restTemplate, 5, "http://localhost", 20, 10);
    }

    @Test
    void createsExpectedResult() {
        IdMappingJobRequest request = new IdMappingJobRequest();
        request.setFrom("UniProtKB_AC-ID");
        request.setTo("EMBL");
        request.setIds("id");
        request.setTaxId("taxId");

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("ids", String.join(",", request.getIds()));
        map.add("from", IdMappingFieldConfig.convertDbNameToPIRDbName(request.getFrom()));
        map.add("to", IdMappingFieldConfig.convertDbNameToPIRDbName(request.getTo()));
        map.add("tax_off", "NO"); // we do not need PIR's header line, "Taxonomy ID:"
        map.add("taxid", request.getTaxId());
        map.add("async", "NO");

        when(restTemplate.postForEntity(
                        "http://localhost", new HttpEntity<>(map, HTTP_HEADERS), String.class))
                .thenReturn(ResponseEntity.ok().body("From1\tTo1\n"));

        IdMappingResult idMappingResult = pirService.mapIds(request, "dummyJobId");
        assertThat(
                idMappingResult.getMappedIds(), contains(new IdMappingStringPair("From1", "To1")));
    }
}
