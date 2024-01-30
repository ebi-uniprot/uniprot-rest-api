package org.uniprot.api.proteome.request;

import static org.uniprot.api.rest.openapi.OpenApiConstants.*;

import javax.validation.constraints.Pattern;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.springdoc.api.annotations.ParameterObject;
import org.uniprot.api.rest.request.StreamRequest;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author lgonzales
 * @since 23/11/2020
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ParameterObject
public class ProteomeStreamRequest extends ProteomeBasicRequest implements StreamRequest {

    @Parameter(description = DOWNLOAD_DESCRIPTION)
    @Pattern(
            regexp = "^true|false$",
            flags = {Pattern.Flag.CASE_INSENSITIVE},
            message = "{stream.invalid.download}")
    private String download;
}
