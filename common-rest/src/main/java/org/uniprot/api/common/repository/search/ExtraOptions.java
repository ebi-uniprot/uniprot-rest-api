package org.uniprot.api.common.repository.search;

import java.util.Collection;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

@Getter
@Builder
public class ExtraOptions {
    private final Collection<String> failedIds;
    @Singular private final Collection<EntryPair<String>> suggestedIds;
    private Integer obsoleteCount;
}
