package org.uniprot.api.idmapping.controller.response;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author sahmad
 * @created 22/02/2021
 */
@Data
@AllArgsConstructor
public class JobStatusResponse {
    private JobStatus jobStatus;
}
