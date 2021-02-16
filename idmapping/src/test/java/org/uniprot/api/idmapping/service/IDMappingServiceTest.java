package org.uniprot.api.idmapping.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.idmapping.controller.request.IDMappingRequest;
import org.uniprot.api.idmapping.model.IDMappingStringPair;

class IDMappingServiceTest {
    @Test
    void checkCanReachPIR() {
        IDMappingService service = new IDMappingService(new RestTemplate());

        IDMappingRequest request =
                IDMappingRequest.builder().id("Q5KPU2").id("Q9PW07").from("ACC").to("EMBL").build();
        QueryResult<IDMappingStringPair> idMappingPairStream = service.fetchIDMappings(request);
        idMappingPairStream
                .getContent()
                .forEach(x -> System.out.println(x.getFromValue() + " -> " + x.getToValue()));
    }
}
