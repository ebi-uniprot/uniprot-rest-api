package org.uniprot.api.idmapping.controller.request;

import javax.validation.constraints.Pattern;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import org.springdoc.api.annotations.ParameterObject;
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
@ParameterObject
public class IdMappingStreamRequest implements StreamRequest {
    @Parameter(
            description =
                    "Default: <tt>false</tt>. Use <tt>true</tt> to download as a file.")
    @Pattern(regexp = "^(?:true|false)$", message = "{idmapping.results.invalid.download}")
    private String download;

    // FAKE fields never used
    private String query;
    private String fields;
    private String sort;
    private String format;
}
