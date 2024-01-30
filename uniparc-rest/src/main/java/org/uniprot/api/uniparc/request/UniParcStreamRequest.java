package org.uniprot.api.uniparc.request;

import javax.validation.constraints.Pattern;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.springdoc.api.annotations.ParameterObject;
import org.uniprot.api.rest.request.StreamRequest;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author lgonzales
 * @since 18/06/2020
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ParameterObject
public class UniParcStreamRequest extends UniParcBasicRequest implements StreamRequest {

    @Parameter(
            description =
                    OpenApiConstants.DOWNLOAD_DESCRIPTION)
    @Pattern(regexp = "^true|false$", message = "{search.uniparc.invalid.download}")
    private String download;
}
