package org.uniprot.api.idmapping.model;

import java.util.List;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

/**
 * Created 17/02/2021
 *
 * @author Edd
 */
@Builder
@Data
public class IdMappingResult {
    @Singular private List<String> unmappedIds;
    @Singular private List<IdMappingStringPair> mappedIds;
}
