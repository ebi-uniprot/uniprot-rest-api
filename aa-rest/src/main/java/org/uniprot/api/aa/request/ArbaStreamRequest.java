package org.uniprot.api.aa.request;

import javax.validation.constraints.Pattern;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.springdoc.api.annotations.ParameterObject;
import org.uniprot.api.rest.request.StreamRequest;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author sahmad
 * @created 19/07/2021
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ParameterObject
public class ArbaStreamRequest extends ArbaBasicRequest implements StreamRequest {
    @Parameter(
            description =
                    "Default: <tt>false</tt>. Use <tt>true</tt> to download as a file.")
    @Pattern(
            regexp = "^(?:true|false)$",
            flags = {Pattern.Flag.CASE_INSENSITIVE},
            message = "{search.aa.invalid.download}")
    private String download;
}
