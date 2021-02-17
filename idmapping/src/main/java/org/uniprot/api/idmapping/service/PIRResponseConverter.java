package org.uniprot.api.idmapping.service;

import static java.util.Collections.emptyList;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.uniprot.api.idmapping.model.IdMappingStringPair;

/**
 * Created 17/02/2021
 *
 * @author Edd
 */
public class PIRResponseConverter {
    public List<IdMappingStringPair> convertToIDMappings(ResponseEntity<String> response) {
        if (response.hasBody()) {
            return response.getBody()
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
                    .collect(Collectors.toList());
        }

        return emptyList();
    }
}
