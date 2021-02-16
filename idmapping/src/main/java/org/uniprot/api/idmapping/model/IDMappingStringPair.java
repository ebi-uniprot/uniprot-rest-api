package org.uniprot.api.idmapping.model;

import lombok.Builder;

/**
 * Created 16/02/2021
 *
 * @author Edd
 */
public class IDMappingStringPair extends IDMappingPair<String> {
    @Builder
    public IDMappingStringPair(String fromValue, String toValue) {
        super(fromValue, toValue);
    }
}
