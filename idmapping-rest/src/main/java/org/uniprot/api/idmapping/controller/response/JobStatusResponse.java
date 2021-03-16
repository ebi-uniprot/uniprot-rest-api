package org.uniprot.api.idmapping.controller.response;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * @author sahmad
 * @created 22/02/2021
 */
@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class JobStatusResponse {
    private final JobStatus jobStatus;
}
