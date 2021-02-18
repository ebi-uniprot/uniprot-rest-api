package org.uniprot.api.idmapping.model;

import lombok.*;

/**
 * Created 16/02/2021
 *
 * @author Edd
 */
@Builder
@Getter
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class IdMappingStringPair {
    private String from;
    private String to;
}
