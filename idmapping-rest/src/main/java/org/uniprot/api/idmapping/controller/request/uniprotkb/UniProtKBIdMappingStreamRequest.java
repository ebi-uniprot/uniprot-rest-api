package org.uniprot.api.idmapping.controller.request.uniprotkb;

import javax.validation.constraints.Pattern;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.springdoc.api.annotations.ParameterObject;
import org.uniprot.api.rest.request.StreamRequest;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author sahmad
 * @created 16/02/2021
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ParameterObject
public class UniProtKBIdMappingStreamRequest extends UniProtKBIdMappingBasicRequest
        implements StreamRequest {
    @Parameter(
            description =
                    "Default: <tt>false</tt>. Use <tt>true</tt> to download as a file.")
    @Pattern(regexp = "^(?:true|false)$", message = "{search.uniprot.invalid.download}")
    private String download;
}
