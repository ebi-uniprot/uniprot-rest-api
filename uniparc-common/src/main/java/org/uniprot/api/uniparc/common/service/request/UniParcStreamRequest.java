package org.uniprot.api.uniparc.common.service.request;

import javax.validation.constraints.Pattern;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.uniprot.api.rest.request.StreamRequest;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author lgonzales
 * @since 18/06/2020
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UniParcStreamRequest extends UniParcBasicRequest implements StreamRequest {

    @Parameter(
            description =
                    "Adds content disposition attachment to response headers, this way it can be downloaded as a file in the browser.")
    @Pattern(regexp = "^true|false$", message = "{search.uniparc.invalid.download}")
    private String download;
}
