package org.uniprot.api.support.data.keyword.request;

import org.uniprot.api.rest.request.StreamRequest;

import javax.validation.constraints.Pattern;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author sahmad
 * @created 21/01/2021
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class KeywordStreamRequest extends KeywordBasicRequest implements StreamRequest {
    @Parameter(
            description =
                    "Adds content disposition attachment to response headers, this way it can be downloaded as a file in the browser.")
    @Pattern(
            regexp = "^true|false$",
            flags = {Pattern.Flag.CASE_INSENSITIVE},
            message = "{stream.invalid.download}")
    private String download;
}
