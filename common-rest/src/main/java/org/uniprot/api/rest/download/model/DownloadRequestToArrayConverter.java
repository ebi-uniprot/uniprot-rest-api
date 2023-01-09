package org.uniprot.api.rest.download.model;

import java.util.function.Function;

import org.uniprot.api.rest.request.StreamRequest;

public class DownloadRequestToArrayConverter implements Function<StreamRequest, char[]> {
    @Override
    public char[] apply(StreamRequest request) {
        StringBuilder builder = new StringBuilder();
        builder.append(request.getQuery().strip().toLowerCase());
        if (request.hasFields()) {
            builder.append(request.getFields().strip().toLowerCase());
        }
        if (request.hasSort()) {
            builder.append(request.getSort().strip().toLowerCase());
        }
        return builder.toString().toCharArray();
    }
}
