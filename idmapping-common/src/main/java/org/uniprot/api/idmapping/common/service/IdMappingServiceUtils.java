package org.uniprot.api.idmapping.common.service;

import java.util.Objects;

import org.uniprot.api.common.repository.search.ExtraOptions;
import org.uniprot.api.idmapping.common.model.IdMappingResult;

public class IdMappingServiceUtils {

    public static ExtraOptions getExtraOptions(IdMappingResult result) {
        ExtraOptions.ExtraOptionsBuilder builder = ExtraOptions.builder();
        if (result.getUnmappedIds() != null) {
            builder.failedIds(result.getUnmappedIds());
        }
        if (result.getSuggestedIds() != null) {
            builder.suggestedIds(result.getSuggestedIds());
        }

        if (Objects.nonNull(result.getObsoleteCount())) {
            builder.obsoleteCount(result.getObsoleteCount());
        }

        return builder.build();
    }
}
