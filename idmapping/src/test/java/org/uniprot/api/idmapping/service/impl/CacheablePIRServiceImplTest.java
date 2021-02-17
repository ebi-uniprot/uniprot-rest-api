package org.uniprot.api.idmapping.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.idmapping.controller.request.IdMappingBasicRequest;
import org.uniprot.api.idmapping.model.IdMappingResult;

class CacheablePIRServiceImplTest {
    @Test
    void canReachPIR() {
        CacheablePIRServiceImpl service = new CacheablePIRServiceImpl(new RestTemplate());
        IdMappingBasicRequest request = new IdMappingBasicRequest();
        request.setFrom("ACC");
        request.setTo("EMBL");
        request.setIds("Q0HIT0,Q3IE36,P12345");

        IdMappingResult result = service.doPIRRequest(request);

        result.getMappedIds()
                .forEach(
                        pair ->
                                System.out.println(
                                        "from=" + pair.getKey() + ", to=" + pair.getValue()));
        System.out.println(result.getUnmappedIds());
    }
}
