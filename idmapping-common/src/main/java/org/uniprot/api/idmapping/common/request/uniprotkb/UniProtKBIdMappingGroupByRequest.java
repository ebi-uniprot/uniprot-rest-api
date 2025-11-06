package org.uniprot.api.idmapping.common.request.uniprotkb;

import org.springdoc.api.annotations.ParameterObject;

import lombok.Data;

@Data
@ParameterObject
public class UniProtKBIdMappingGroupByRequest {
    private final String query;

    private final String parentId;
}
