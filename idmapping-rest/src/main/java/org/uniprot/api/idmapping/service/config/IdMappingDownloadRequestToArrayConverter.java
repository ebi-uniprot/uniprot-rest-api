package org.uniprot.api.idmapping.service.config;

import java.util.Objects;
import java.util.function.Function;

import org.uniprot.api.idmapping.controller.request.IdMappingDownloadRequest;

public class IdMappingDownloadRequestToArrayConverter
        implements Function<IdMappingDownloadRequest, char[]> {

    @Override
    public char[] apply(IdMappingDownloadRequest request) {
        StringBuilder builder = new StringBuilder();

        if (Objects.nonNull(request.getJobId())) {
            builder.append(request.getJobId().strip().toLowerCase());
        }
        if (Objects.nonNull(request.getFormat())) {
            builder.append(request.getFormat().strip().toLowerCase());
        }
        if (Objects.nonNull(request.getFields())) {
            builder.append(request.getFields().strip().toLowerCase());
        }

        return builder.toString().toCharArray();
    }
}
