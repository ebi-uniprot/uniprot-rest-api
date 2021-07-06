package org.uniprot.api.idmapping.service;

import org.uniprot.api.idmapping.controller.request.IdMappingJobRequest;
import org.uniprot.api.idmapping.controller.response.JobSubmitResponse;
import org.uniprot.api.idmapping.model.IdMappingJob;

/**
 * Created 25/02/2021
 *
 * @author Edd
 */
public interface IdMappingJobService {
    JobSubmitResponse submitJob(IdMappingJobRequest request);

    String getRedirectPathToResults(IdMappingJob job, String requestUrl);
}
