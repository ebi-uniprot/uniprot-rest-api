package org.uniprot.api.uniparc.request;

import static org.uniprot.api.rest.openapi.OpenAPIConstants.PROTEOME_UPID_UNIPARC_DESCRIPTION;
import static org.uniprot.api.rest.openapi.OpenAPIConstants.PROTEOME_UPID_UNIPARC_EXAMPLE;

import javax.validation.constraints.NotNull;

import org.springdoc.api.annotations.ParameterObject;
import org.uniprot.api.rest.request.StreamRequest;
import org.uniprot.api.uniparc.common.service.request.UniParcGetByIdStreamRequest;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@ParameterObject
public class UniParcGetByProteomeIdStreamRequest extends UniParcGetByIdStreamRequest
        implements StreamRequest {
    @Parameter(hidden = true)
    private static final String PROTEOME_ID_STR = "upid";

    @Parameter(
            description = PROTEOME_UPID_UNIPARC_DESCRIPTION,
            example = PROTEOME_UPID_UNIPARC_EXAMPLE)
    @NotNull(message = "{search.required}")
    private String upId;

    @Override
    public String getQuery() {
        return PROTEOME_ID_STR + ":" + this.upId;
    }
}
