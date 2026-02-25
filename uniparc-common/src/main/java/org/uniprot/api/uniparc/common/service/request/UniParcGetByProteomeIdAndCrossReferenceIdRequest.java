package org.uniprot.api.uniparc.common.service.request;

import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;
import static org.uniprot.api.rest.openapi.OpenAPIConstants.ID_UNIPARC_EXAMPLE;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.springdoc.api.annotations.ParameterObject;
import org.uniprot.store.search.field.validator.FieldRegexConstants;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

@Data
@ParameterObject
public class UniParcGetByProteomeIdAndCrossReferenceIdRequest {
    @Pattern(
            regexp = FieldRegexConstants.UNIPARC_UPI_REGEX,
            flags = {Pattern.Flag.CASE_INSENSITIVE},
            message = "{search.invalid.upi.value}")
    @NotNull(message = "{search.required}")
    @Parameter(description = ID_UNIPARC_DESCRIPTION, example = ID_UNIPARC_EXAMPLE)
    private String upi;

    @NotNull(message = "{search.required}")
    private String xrefId;
}
