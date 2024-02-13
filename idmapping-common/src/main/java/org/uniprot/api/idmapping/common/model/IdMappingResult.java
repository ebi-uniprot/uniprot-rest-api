package org.uniprot.api.idmapping.common.model;

import java.io.Serializable;
import java.util.List;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import org.uniprot.api.common.repository.search.ProblemPair;
import org.uniprot.api.idmapping.common.response.model.IdMappingStringPair;

/**
 * Created 17/02/2021
 *
 * @author Edd
 */
@Builder
@Data
public class IdMappingResult implements Serializable {

    private static final long serialVersionUID = -3638209244179967840L;
    @Singular private List<String> unmappedIds;
    @Singular private List<IdMappingStringPair> suggestedIds;
    @Singular private List<IdMappingStringPair> mappedIds;
    @Singular private List<ProblemPair> warnings;
    @Singular private List<ProblemPair> errors;

    private Integer obsoleteCount;
}
