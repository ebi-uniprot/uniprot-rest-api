package org.uniprot.api.uniparc.request;

import javax.validation.constraints.Positive;

import lombok.Data;
import lombok.EqualsAndHashCode;
import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author sahmad
 * @created 13/08/2020
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UniParcGetByIdPageRequest extends UniParcGetByIdRequest {
    @Parameter(description = "Size of the result. Defaults to 25")
    @Positive(message = "{search.positive}")
    private Integer size;

    @Parameter(hidden = true)
    private String cursor;
}
