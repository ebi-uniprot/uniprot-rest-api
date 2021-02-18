package org.uniprot.api.idmapping.model;

import lombok.AllArgsConstructor;
import lombok.Builder;

import lombok.Getter;
import lombok.ToString;
import org.uniprot.core.util.PairImpl;

/**
 * Created 16/02/2021
 *
 * @author Edd
 */
@Builder
@Getter
@AllArgsConstructor
@ToString
public class IdMappingStringPair {
    private String from;
    private String to;
}
