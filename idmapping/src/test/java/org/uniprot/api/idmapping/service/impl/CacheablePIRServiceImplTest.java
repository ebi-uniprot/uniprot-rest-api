package org.uniprot.api.idmapping.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.idmapping.controller.request.IdMappingBasicRequest;
import org.uniprot.api.idmapping.service.PIRResponseConverter;

class CacheablePIRServiceImplTest {
    @Test
    void canReachPIR() {
        CacheablePIRServiceImpl service = new CacheablePIRServiceImpl(new RestTemplate());
        IdMappingBasicRequest request = new IdMappingBasicRequest();
        request.setFrom("ACC");
        request.setTo("EMBL");
        request.setIds("Q0HIT0,Q3IE36,P12345");

        ResponseEntity<String> responseEntity = service.doPIRRequest(request);
        PIRResponseConverter converter = new PIRResponseConverter();

        System.out.println(converter.convertToIDMappings(responseEntity));
    }
}
