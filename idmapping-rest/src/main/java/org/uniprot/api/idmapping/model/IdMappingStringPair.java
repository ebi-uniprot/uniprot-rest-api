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
public class IdMappingStringPair implements EntryPair<String> {
    private static final long serialVersionUID = -2255505407227241111L;
    private final String from;
    private final String to;
}
