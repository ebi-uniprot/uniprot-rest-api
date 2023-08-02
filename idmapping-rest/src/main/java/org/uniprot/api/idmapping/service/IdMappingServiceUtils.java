package org.uniprot.api.idmapping.service;

import org.uniprot.api.common.repository.search.ExtraOptions;
import org.uniprot.api.idmapping.model.IdMappingResult;

public class IdMappingServiceUtils {

    public static ExtraOptions getExtraOptions(IdMappingResult result) {
        ExtraOptions.ExtraOptionsBuilder builder = ExtraOptions.builder();
        if (result.getUnmappedIds() != null) {
            builder.failedIds(result.getUnmappedIds());
        }
        if (result.getSuggestedIds() != null) {
            builder.suggestedIds(result.getSuggestedIds());
        }
        return builder.build();
    }
}
