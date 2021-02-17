package org.uniprot.api.idmapping.model;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.ArrayList;
import java.util.List;

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

//    public static class IdMappingResultBuilder {
//        private List<String> unmappedIds = new ArrayList<>();
//        private List<IdMappingStringPair> mappedIds = new ArrayList<>();
//    }
}
