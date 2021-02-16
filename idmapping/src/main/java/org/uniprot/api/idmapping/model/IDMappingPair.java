package org.uniprot.api.idmapping.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

//@Builder
@Getter
@AllArgsConstructor
abstract class IDMappingPair<T> {
    private final String fromValue;
    private final T toValue;
}
