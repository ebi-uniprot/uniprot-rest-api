package org.uniprot.api.uniprotkb.common.service.uniprotkb.request;

import static org.uniprot.api.rest.openapi.OpenApiConstants.DOWNLOAD_DESCRIPTION;

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
public class UniProtKBStreamRequest extends UniProtKBBasicRequest implements StreamRequest {

    @Parameter(description = DOWNLOAD_DESCRIPTION)
    @Pattern(
            regexp = "^true$|^false$",
            flags = {Pattern.Flag.CASE_INSENSITIVE},
            message = "{search.uniprot.invalid.download}")
    private String download;

    @Parameter(hidden = true)
    private boolean isLargeSolrStreamRestricted = true;
}
