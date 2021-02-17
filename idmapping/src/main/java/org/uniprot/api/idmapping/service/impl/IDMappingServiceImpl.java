package org.uniprot.api.idmapping.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.idmapping.controller.request.IDMappingRequest;
import org.uniprot.api.idmapping.model.IDMappingStringPair;
import org.uniprot.api.idmapping.service.IDMappingPIRService;
import org.uniprot.api.idmapping.service.IDMappingService;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created 08/02/2021
 *
 * @author Edd
 */
@Service
public class IDMappingServiceImpl implements IDMappingService {

    private final IDMappingPIRService pirService;

    @Autowired
    public IDMappingServiceImpl(IDMappingPIRService pirService) {
        this.pirService = pirService;
    }

    @Override
    public QueryResult<IDMappingStringPair> fetchIDMappings(IDMappingRequest request) {
        ResponseEntity<String> response = pirService.doPIRRequest(request);

        if (response.hasBody()) {
            Stream<IDMappingStringPair> idMappingPairStream =
                    response.getBody()
                            .lines()
                            .filter(x -> x.contains("\t"))
                            .map(row -> row.split("\t"))
                            .map(
                                    linePart -> {
                                        String fromValue = linePart[0];
                                        return Arrays.stream(linePart[1].split(";"))
                                                .map(
                                                        toValue ->
                                                                IDMappingStringPair.builder()
                                                                        .fromValue(fromValue)
                                                                        .toValue(toValue)
                                                                        .build())
                                                .collect(Collectors.toList());
                                    })
                            .flatMap(Collection::stream);

            return QueryResult.of(idMappingPairStream, null);
        }

        return QueryResult.of(Stream.empty(), null);
    }
}
