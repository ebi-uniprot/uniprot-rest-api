package org.uniprot.api.aa.request;

import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;

import javax.validation.constraints.Pattern;

import org.springdoc.api.annotations.ParameterObject;
import org.uniprot.api.rest.request.StreamRequest;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author sahmad
 * @created 19/07/2021
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ParameterObject
public class ArbaStreamRequest extends ArbaBasicRequest implements StreamRequest {
    @Parameter(description = DOWNLOAD_DESCRIPTION)
    @Pattern(
            regexp = "^true$|^false$",
            flags = {Pattern.Flag.CASE_INSENSITIVE},
            message = "{search.aa.invalid.download}")
    private String download;
}
