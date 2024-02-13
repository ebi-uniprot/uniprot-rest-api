package org.uniprot.api.idmapping.common.service;

import org.uniprot.api.idmapping.common.model.IdMappingJob;
import org.uniprot.api.rest.output.job.JobSubmitResponse;
import org.uniprot.api.rest.request.idmapping.IdMappingJobRequest;

/**
 * Created 25/02/2021
 *
 * @author Edd
 */
public interface IdMappingJobService {
    String UNIREF_ID_MAPPING_PATH = "uniref";
    String UNIPARC_ID_MAPPING_PATH = "uniparc";
    String UNIPROTKB_ID_MAPPING_PATH = "uniprotkb";

    String IDMAPPING_PATH = "/idmapping";

    JobSubmitResponse submitJob(IdMappingJobRequest request);

    String getRedirectPathToResults(IdMappingJob job, String requestUrl);
}
