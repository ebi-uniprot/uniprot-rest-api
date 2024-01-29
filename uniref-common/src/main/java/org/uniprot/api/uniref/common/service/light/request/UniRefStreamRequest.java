package org.uniprot.api.uniref.common.service.light.request;

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
public class UniRefStreamRequest extends UniRefBasicRequest implements StreamRequest {

    @Parameter(
            description =
                    "Adds content disposition attachment to response headers, this way it can be downloaded as a file in the browser.")
    @Pattern(
            regexp = "^true$|^false$",
            flags = {Pattern.Flag.CASE_INSENSITIVE},
            message = "{search.uniref.invalid.download}")
    private String download;

    private boolean isLargeSolrStreamRestricted = true;
}
