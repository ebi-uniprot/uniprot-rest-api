package org.uniprot.api.idmapping.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Created 17/02/2021
 *
 * @author Edd
 */
@Builder
@Data
public class IdMappingResult {
    private List<String> unmappedIds;
    private List<IdMappingStringPair> mappedIds;
}
