package org.uniprot.api.idmapping.service;

import org.uniprot.api.idmapping.controller.request.IdMappingBasicRequest;
import org.uniprot.api.idmapping.controller.response.JobSubmitResponse;
import org.uniprot.api.idmapping.model.IdMappingJob;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

/**
 * Created 25/02/2021
 *
 * @author Edd
 */
public interface IdMappingJobService {
    JobSubmitResponse submitJob(IdMappingBasicRequest request)
            throws InvalidKeySpecException, NoSuchAlgorithmException;

    String getRedirectPathToResults(IdMappingJob job);
}
