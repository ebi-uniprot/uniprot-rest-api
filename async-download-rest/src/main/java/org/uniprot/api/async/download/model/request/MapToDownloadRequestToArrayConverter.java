package org.uniprot.api.async.download.model.request;

import java.util.Objects;
import java.util.function.Function;

import org.uniprot.api.async.download.model.request.mapto.MapToDownloadRequest;

public class MapToDownloadRequestToArrayConverter<T extends MapToDownloadRequest>
        implements Function<T, char[]> {

    @Override
    public char[] apply(T request) {
        StringBuilder builder = DownloadRequestToArrayConverter.buildString(request);
        if (Objects.nonNull(request.getFrom())) {
            builder.append(request.getFrom().toLowerCase());
        }
        if (Objects.nonNull(request.getTo())) {
            builder.append(request.getTo().toLowerCase());
        }
        return builder.toString().toCharArray();
    }
}
