package org.uniprot.api.common.repository.search;

import java.util.Collection;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class IdMappingStatistics {
    private final Collection<String> failedIds;
    private final Collection<String> suggestedIds;
}
