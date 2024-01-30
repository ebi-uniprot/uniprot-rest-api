package org.uniprot.api.idmapping.controller.request.uniparc;

import javax.validation.constraints.Pattern;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.springdoc.api.annotations.ParameterObject;
import org.uniprot.api.rest.request.StreamRequest;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author lgonzales
 * @since 25/02/2021
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ParameterObject
public class UniParcIdMappingStreamRequest extends UniParcIdMappingBasicRequest
        implements StreamRequest {

    @Parameter(
            description =
                    OpenApiConstants.DOWNLOAD_DESCRIPTION)
    @Pattern(regexp = "^(?:true|false)$", message = "{search.uniparc.invalid.download}")
    private String download;
}
