package org.uniprot.api.rest.output;

import org.uniprot.api.common.repository.search.EntryPair;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class FakePair implements EntryPair<String> {
    private final String from;
    private final String to;
}
