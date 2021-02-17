package org.uniprot.api.idmapping.service;

import org.junit.jupiter.api.Test;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.idmapping.controller.request.IDMappingRequest;
import org.uniprot.api.idmapping.model.IDMappingStringPair;
import org.uniprot.api.idmapping.service.impl.IDMappingServiceImpl;

class IDMappingServiceTest {
    @Test
    void checkCanReachPIR() {
        IDMappingServiceImpl service = new IDMappingServiceImpl(null);

        IDMappingRequest request =
                IDMappingRequest.builder().id("Q5KPU2").id("Q9PW07").from("ACC").to("EMBL").build();
        QueryResult<IDMappingStringPair> idMappingPairStream = service.fetchIDMappings(request);
        idMappingPairStream
                .getContent()
                .forEach(x -> System.out.println(x.getFromValue() + " -> " + x.getToValue()));
    }
}
