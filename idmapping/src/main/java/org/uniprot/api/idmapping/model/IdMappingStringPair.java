package org.uniprot.api.idmapping.model;

import lombok.Builder;

import org.uniprot.core.util.PairImpl;

/**
 * Created 16/02/2021
 *
 * @author Edd
 */
public class IdMappingStringPair extends PairImpl<String, String> {
    @Builder
    public IdMappingStringPair(String fromValue, String toValue) {
        super(fromValue, toValue);
    }
}
