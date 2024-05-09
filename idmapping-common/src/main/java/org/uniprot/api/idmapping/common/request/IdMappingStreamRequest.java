package org.uniprot.api.idmapping.common.request;

import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;

import javax.validation.constraints.Pattern;

import org.springdoc.api.annotations.ParameterObject;
import org.uniprot.api.rest.request.StreamRequest;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * Created 20/05/2021
 *
 * @author Edd
 */
@Data
@AllArgsConstructor
@Builder(toBuilder = true)
@ParameterObject
public class IdMappingStreamRequest implements StreamRequest {
    @Parameter(description = DOWNLOAD_DESCRIPTION)
    @Pattern(regexp = "^(?:true|false)$", message = "{idmapping.results.invalid.download}")
    private String download;

    // FAKE fields never used
    @Parameter(hidden = true)
    private String query;

    @Parameter(hidden = true)
    private String fields;

    @Parameter(hidden = true)
    private String sort;

    @Parameter(hidden = true)
    private String format;
}
