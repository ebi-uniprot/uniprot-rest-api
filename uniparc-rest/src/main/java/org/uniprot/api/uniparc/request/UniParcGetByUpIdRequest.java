package org.uniprot.api.uniparc.request;

import javax.validation.constraints.NotNull;

import lombok.Data;
import lombok.EqualsAndHashCode;
import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author sahmad
 * @created 13/08/2020
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UniParcGetByUpIdRequest extends UniParcGetByIdRequest {
    @Parameter(description = "UniProt Proteome UPID")
    @NotNull(message = "{search.required}")
    private String upId;
}
