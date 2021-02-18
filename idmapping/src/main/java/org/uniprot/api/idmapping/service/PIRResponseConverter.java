package org.uniprot.api.idmapping.service;

import java.util.Arrays;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.uniprot.api.idmapping.model.IdMappingResult;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.api.idmapping.service.impl.CacheablePIRServiceImpl;
import org.uniprot.core.util.Utils;

/**
 * Created 17/02/2021
 *
 * @author Edd
 */
public class PIRResponseConverter {
    public IdMappingResult convertToIDMappings(ResponseEntity<String> response) {
        IdMappingResult.IdMappingResultBuilder builder = IdMappingResult.builder();
        HttpStatus statusCode = response.getStatusCode();
        if (statusCode.equals(HttpStatus.OK)) {
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
                                                                        .from(fromValue)
                                                                        .to(toValue)
                                                                        .build())
                                                .forEach(builder::mappedId);
                                    }
                                });
            }
        } else {
            throw new HttpServerErrorException(
                    statusCode,
                    "PIR id-mapping service error: " + CacheablePIRServiceImpl.PIR_ID_MAPPING_URL);
        }

        return builder.build();
    }
}
