package org.uniprot.api.idmapping.service;

import org.springframework.http.ResponseEntity;
import org.uniprot.api.idmapping.model.IdMappingResult;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.core.util.Utils;

import java.util.Arrays;

/**
 * Created 17/02/2021
 *
 * @author Edd
 */
public class PIRResponseConverter {
    public IdMappingResult convertToIDMappings(ResponseEntity<String> response) {
        IdMappingResult.IdMappingResultBuilder builder = IdMappingResult.builder();
        if (response.hasBody()) {
            response.getBody()
                    .lines()
                    .filter(line -> !line.startsWith("Taxonomy ID:"))
                    .filter(Utils::notNullNotEmpty)
                    .filter(line -> !line.startsWith("MSG:"))
                    .forEach(
                            line -> {
                                String[] rowParts = line.split("\t");
                                if (rowParts.length == 1) {
                                    builder.unmappedId(rowParts[0]);
                                } else {
                                    String fromValue = rowParts[0];
                                    Arrays.stream(rowParts[1].split(";"))
                                            .map(
                                                    toValue ->
                                                            IdMappingStringPair.builder()
                                                                    .fromValue(fromValue)
                                                                    .toValue(toValue)
                                                                    .build())
                                            .forEach(builder::mappedId);
                                }
                            });
        }

        return builder.build();
    }
}
