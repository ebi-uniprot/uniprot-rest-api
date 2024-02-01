package org.uniprot.api.idmapping.common.request;

import javax.validation.constraints.Pattern;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import org.uniprot.api.rest.request.StreamRequest;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * Created 20/05/2021
 *
 * @author Edd
 */
@Data
@AllArgsConstructor
@Builder(toBuilder = true)
public class IdMappingStreamRequest implements StreamRequest {
    @Parameter(
            description =
                    "Adds content disposition attachment to response headers, this way it can be downloaded as a file in the browser.")
    @Pattern(regexp = "^(?:true|false)$", message = "{idmapping.results.invalid.download}")
    private String download;

    // FAKE fields never used
    private String query;
    private String fields;
    private String sort;
    private String format;
}
