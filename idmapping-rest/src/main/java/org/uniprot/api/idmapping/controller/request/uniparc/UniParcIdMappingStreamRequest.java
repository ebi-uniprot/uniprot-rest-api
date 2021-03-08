package org.uniprot.api.idmapping.controller.request.uniparc;

import javax.validation.constraints.Pattern;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.uniprot.api.rest.request.StreamRequest;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author lgonzales
 * @since 25/02/2021
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UniParcIdMappingStreamRequest extends UniParcIdMappingBasicRequest
        implements StreamRequest {

    @Parameter(
            description =
                    "Adds content disposition attachment to response headers, this way it can be downloaded as a file in the browser.")
    @Pattern(regexp = "^true|false$", message = "{search.uniparc.invalid.download}")
    private String download;
}
