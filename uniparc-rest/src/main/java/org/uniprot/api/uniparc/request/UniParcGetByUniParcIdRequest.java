package org.uniprot.api.uniparc.request;

import static org.uniprot.api.rest.openapi.OpenApiConstants.*;
import static org.uniprot.api.rest.openapi.OpenApiConstants.FIELDS_UNIPARC_DESCRIPTION;
import static org.uniprot.api.rest.openapi.OpenApiConstants.FIELDS_UNIPARC_EXAMPLE;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.springdoc.api.annotations.ParameterObject;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.search.field.validator.FieldRegexConstants;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author sahmad
 * @created 14/08/2020
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ParameterObject
public class UniParcGetByUniParcIdRequest extends UniParcGetByIdRequest {

    @Pattern(
            regexp = FieldRegexConstants.UNIPARC_UPI_REGEX,
            flags = {Pattern.Flag.CASE_INSENSITIVE},
            message = "{search.invalid.upi.value}")
    @NotNull(message = "{search.required}")
    @Parameter(description = ID_UNIPARC_DESCRIPTION, example = ID_UNIPARC_EXAMPLE)
    private String upi;

    @Parameter(description = FIELDS_UNIPARC_DESCRIPTION, example = FIELDS_UNIPARC_EXAMPLE)
    @ValidReturnFields(uniProtDataType = UniProtDataType.UNIPARC)
    private String fields;
}
