package org.uniprot.api.async.download.model.request;

import java.util.Objects;
import java.util.function.Function;

public class DownloadRequestToArrayConverter<T extends SolrStreamDownloadRequest>
        implements Function<T, char[]> {

    @Override
    public char[] apply(T request) {
        StringBuilder builder = buildString(request);
        return builder.toString().toCharArray();
    }

    public static <T extends SolrStreamDownloadRequest> StringBuilder buildString(T request) {
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
        return builder;
    }
}
