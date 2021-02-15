package org.uniprot.api.idmapping.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.List;

@Builder
@Getter
public class IDMappingPair {
    private final String fromValue;
    @Singular private final List<String> toValues;
}
