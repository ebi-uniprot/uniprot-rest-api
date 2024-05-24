package org.uniprot.api.async.download.model.idmapping;

import java.util.Objects;
import java.util.function.Function;

public class IdMappingDownloadRequestToArrayConverter
        implements Function<org.uniprot.api.async.download.refactor.request.idmapping.IdMappingDownloadRequest, char[]> {

    @Override
    public char[] apply(org.uniprot.api.async.download.refactor.request.idmapping.IdMappingDownloadRequest request) {
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
