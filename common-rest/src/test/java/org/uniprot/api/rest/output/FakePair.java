package org.uniprot.api.rest.output;

import lombok.Builder;
import lombok.Getter;

import org.uniprot.api.common.repository.search.EntryPair;

@Builder
@Getter
public class FakePair implements EntryPair<String> {
    private final String from;
    private final String to;
}
