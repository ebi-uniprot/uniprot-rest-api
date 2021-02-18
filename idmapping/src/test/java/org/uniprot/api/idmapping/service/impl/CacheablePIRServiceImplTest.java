package org.uniprot.api.idmapping.service.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.uniprot.api.idmapping.service.impl.CacheablePIRServiceImpl.HTTP_HEADERS;
import static org.uniprot.api.idmapping.service.impl.CacheablePIRServiceImpl.PIR_ID_MAPPING_URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.idmapping.controller.request.IdMappingBasicRequest;
import org.uniprot.api.idmapping.model.IdMappingResult;
import org.uniprot.api.idmapping.model.IdMappingStringPair;

class CacheablePIRServiceImplTest {

    private RestTemplate restTemplate;
    private CacheablePIRServiceImpl pirService;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        pirService = new CacheablePIRServiceImpl(restTemplate);
    }

    @Test
    void createsExpectedResult() {
        IdMappingBasicRequest request = new IdMappingBasicRequest();
        request.setFrom("from");
        request.setTo("to");
        request.setIds("id");
        request.setTaxId("taxId");

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("ids", String.join(",", request.getIds()));
        map.add("from", request.getFrom());
        map.add("to", request.getTo());
        map.add("tax_off", "NO"); // we do not need PIR's header line, "Taxonomy ID:"
        map.add("taxid", request.getTaxId());
        map.add("async", "NO");

        when(restTemplate.postForEntity(
                        PIR_ID_MAPPING_URL, new HttpEntity<>(map, HTTP_HEADERS), String.class))
                .thenReturn(ResponseEntity.ok().body("From1\tTo1\n"));

        IdMappingResult idMappingResult = pirService.doPIRRequest(request);

        assertThat(
                idMappingResult.getMappedIds(), contains(new IdMappingStringPair("From1", "To1")));
    }
}
