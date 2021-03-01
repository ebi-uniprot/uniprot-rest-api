package org.uniprot.api.idmapping.model;

import lombok.*;
import org.uniprot.core.uniparc.UniParcEntry;

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
public class IdMappingStringPair implements EntryPair<String> {
    private final String from;
    private final String to;
}
