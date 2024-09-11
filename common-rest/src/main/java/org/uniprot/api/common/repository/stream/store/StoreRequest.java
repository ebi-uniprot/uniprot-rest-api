package org.uniprot.api.common.repository.stream.store;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StoreRequest {

    private final boolean addLineage;

    private final String fields;
}
