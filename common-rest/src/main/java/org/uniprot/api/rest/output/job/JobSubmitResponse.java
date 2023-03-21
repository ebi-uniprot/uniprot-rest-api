package org.uniprot.api.rest.output.job;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author sahmad
 * @created 22/02/2021
 */
@Getter
@AllArgsConstructor
public class JobSubmitResponse {
    private final String jobId;
}
