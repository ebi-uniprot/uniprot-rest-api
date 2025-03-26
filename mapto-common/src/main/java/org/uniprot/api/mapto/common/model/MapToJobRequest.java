package org.uniprot.api.mapto.common.model;

import lombok.Data;
import org.uniprot.api.mapto.common.search.MapToSearchService;
import org.uniprot.api.mapto.common.service.MapToJobService;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.store.config.UniProtDataType;

import java.util.List;

@Data
public class MapToJobRequest {
    private final UniProtDataType source;
    private final UniProtDataType target;
    private final String query;
}
