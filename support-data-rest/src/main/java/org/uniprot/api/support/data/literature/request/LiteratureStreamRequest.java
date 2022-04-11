package org.uniprot.api.support.data.literature.request;

import javax.validation.constraints.Pattern;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.uniprot.api.rest.request.StreamRequest;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author sahmad
 * @created 22/01/2021
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LiteratureStreamRequest extends LiteratureBasicRequest implements StreamRequest {
    @Parameter(
            hidden = true,
            description =
                    "Adds content disposition attachment to response headers, this way it can be downloaded as a file in the browser.")
    @Pattern(
            regexp = "^(?:true|false)$",
            flags = {Pattern.Flag.CASE_INSENSITIVE},
            message = "{stream.invalid.download}")
    private String download;
}
