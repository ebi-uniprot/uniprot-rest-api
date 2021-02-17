package org.uniprot.api.idmapping.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.api.idmapping.model.IdMappingResult;

/**
 * Created 17/02/2021
 *
 * @author Edd
 */
public class PIRResponseConverter {
    public IdMappingResult convertToIDMappings(ResponseEntity<String> response) {
        IdMappingResult.IdMappingResultBuilder builder = IdMappingResult.builder();
        if (response.hasBody()) {
            builder.mappedIds(response.getBody()
                    .lines()
                    .filter(x -> x.contains("\t"))
                    .map(row -> row.split("\t"))
                    .map(
                            linePart -> {
                                String fromValue = linePart[0];
                                return Arrays.stream(linePart[1].split(";"))
                                        .map(
                                                toValue ->
                                                        IdMappingStringPair.builder()
                                                                .fromValue(fromValue)
                                                                .toValue(toValue)
                                                                .build())
                                        .collect(Collectors.toList());
                            })
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList()));
        }

        return builder.build();
    }
}
