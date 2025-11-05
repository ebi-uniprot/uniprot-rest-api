package org.uniprot.api.idmapping.common.request.uniprotkb;

import lombok.Data;
import org.springdoc.api.annotations.ParameterObject;


@Data
@ParameterObject
public class UniProtKBIdMappingGroupByRequest {
    private final String query;

    private final String parentId;
}
