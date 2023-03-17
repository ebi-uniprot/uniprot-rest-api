package org.uniprot.api.idmapping.service;

import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.rest.output.job.JobSubmitResponse;
import org.uniprot.api.rest.request.idmapping.IdMappingJobRequest;

/**
 * Created 25/02/2021
 *
 * @author Edd
 */
public interface IdMappingJobService {
    JobSubmitResponse submitJob(IdMappingJobRequest request);

    String getRedirectPathToResults(IdMappingJob job, String requestUrl);
}
