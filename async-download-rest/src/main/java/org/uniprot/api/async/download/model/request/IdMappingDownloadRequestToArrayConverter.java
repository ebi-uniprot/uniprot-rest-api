package org.uniprot.api.async.download.model.request;

import java.util.Objects;
import java.util.function.Function;

import org.uniprot.api.async.download.model.request.idmapping.IdMappingDownloadRequest;

public class IdMappingDownloadRequestToArrayConverter
        implements Function<IdMappingDownloadRequest, char[]> {

    @Override
    public char[] apply(IdMappingDownloadRequest request) {
        StringBuilder builder = new StringBuilder();

        if (Objects.nonNull(request.getIdMappingJobId())) {
            builder.append(request.getIdMappingJobId().strip().toLowerCase());
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
