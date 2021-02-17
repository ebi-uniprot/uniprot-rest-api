package org.uniprot.api.idmapping.controller.request;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.uniprot.api.rest.request.StreamRequest;

import javax.validation.constraints.Pattern;

/**
 * @author sahmad
 * @created 16/02/2021
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class IdMappingStreamRequest extends IdMappingBasicRequest implements StreamRequest {
    @Parameter(
            description =
                    "Adds content disposition attachment to response headers, this way it can be downloaded as a file in the browser.")
    @Pattern(regexp = "^true|false$", message = "{search.uniparc.invalid.download}")
    private String download;

    @Override
    public String getQuery() {
        return "";
    }
}
