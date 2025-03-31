package org.uniprot.api.mapto.common.model;

import org.uniprot.store.config.UniProtDataType;

import lombok.Data;

import java.util.Map;

@Data
public class MapToJobRequest {
    private final UniProtDataType source;
    private final UniProtDataType target;
    private final String query;
    private Map<String,String> extraParams;
}
