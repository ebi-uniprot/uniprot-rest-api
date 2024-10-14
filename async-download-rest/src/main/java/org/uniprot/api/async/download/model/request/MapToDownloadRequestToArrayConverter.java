package org.uniprot.api.async.download.model.request;

import java.util.Objects;
import java.util.function.Function;

import org.uniprot.api.async.download.model.request.mapto.MapToDownloadRequest;

public class MapToDownloadRequestToArrayConverter<T extends MapToDownloadRequest>
        implements Function<T, char[]> {

    @Override
    public char[] apply(T request) {
        StringBuilder builder = new StringBuilder();
        builder.append(request.getQuery().strip().toLowerCase());
        if (Objects.nonNull(request.getFields())) {
            builder.append(request.getFields().strip().toLowerCase());
        }
        if (Objects.nonNull(request.getSort())) {
            builder.append(request.getSort().strip().toLowerCase());
        }
        if (Objects.nonNull(request.getFormat())) {
            builder.append(request.getFormat().toLowerCase());
        }
        if (Objects.nonNull(request.getFrom())) {
            builder.append(request.getFrom().toLowerCase());
        }
        if (Objects.nonNull(request.getTo())) {
            builder.append(request.getTo().toLowerCase());
        }
        return builder.toString().toCharArray();
    }
}
