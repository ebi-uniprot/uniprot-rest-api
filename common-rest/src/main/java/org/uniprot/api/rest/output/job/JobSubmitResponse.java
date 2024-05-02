package org.uniprot.api.rest.output.job;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author sahmad
 * @created 22/02/2021
 */
@Getter
@AllArgsConstructor
@RequiredArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class JobSubmitResponse {
    private final String jobId;
    private String message;
}
