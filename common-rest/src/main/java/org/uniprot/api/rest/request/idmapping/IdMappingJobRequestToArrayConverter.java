package org.uniprot.api.rest.request.idmapping;

import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.uniprot.core.util.Utils;

public class IdMappingJobRequestToArrayConverter implements Function<IdMappingJobRequest, char[]> {
    @Override
    public char[] apply(IdMappingJobRequest request) {
        StringBuilder builder = new StringBuilder();
        builder.append(request.getFrom().strip().toLowerCase());
        builder.append(request.getTo().strip().toLowerCase());
        builder.append(
                Stream.of(request.getIds().strip().split(","))
                        .map(String::toLowerCase)
                        .map(String::strip)
                        .collect(Collectors.joining(",")));

        if (Utils.notNullNotEmpty(request.getTaxId())) {
            builder.append(request.getTaxId().strip().toLowerCase());
        }

        return builder.toString().toCharArray();
    }
}
