package org.uniprot.api.mapto.common.service;

import java.util.function.Function;

import org.uniprot.api.mapto.common.model.MapToJobRequest;
import org.uniprot.api.rest.request.HashGenerator;

public class MapToHashGenerator extends HashGenerator<MapToJobRequest> {
    public MapToHashGenerator(
            Function<MapToJobRequest, char[]> requestToArrayConverter, String salt) {
        super(requestToArrayConverter, salt);
    }
}
