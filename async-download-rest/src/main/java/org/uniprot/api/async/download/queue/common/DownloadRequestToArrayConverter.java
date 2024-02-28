package org.uniprot.api.async.download.queue.common;

import java.util.Objects;
import java.util.function.Function;

import org.uniprot.api.async.download.model.DownloadRequest;

public class DownloadRequestToArrayConverter implements Function<DownloadRequest, char[]> {
    @Override
    public char[] apply(DownloadRequest request) {
        StringBuilder builder = new StringBuilder();
        builder.append(request.getQuery().strip().toLowerCase());
        if (request.hasFields()) {
            builder.append(request.getFields().strip().toLowerCase());
        }
        if (request.hasSort()) {
            builder.append(request.getSort().strip().toLowerCase());
        }

        if (Objects.nonNull(request.getFormat())) {
            builder.append(request.getFormat().toLowerCase());
        }
        return builder.toString().toCharArray();
    }
}
