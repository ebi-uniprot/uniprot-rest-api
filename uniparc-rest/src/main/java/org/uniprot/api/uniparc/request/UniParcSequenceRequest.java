package org.uniprot.api.uniparc.request;

import static org.uniprot.api.rest.openapi.OpenApiConstants.*;
import static org.uniprot.api.rest.openapi.OpenApiConstants.FIELDS_UNIPARC_DESCRIPTION;
import static org.uniprot.api.rest.openapi.OpenApiConstants.FIELDS_UNIPARC_EXAMPLE;
import static org.uniprot.store.search.field.validator.FieldRegexConstants.*;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.springdoc.api.annotations.ParameterObject;
import org.uniprot.api.rest.openapi.OpenApiConstants;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.store.config.UniProtDataType;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author lgonzales
 * @since 19/08/2020
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ParameterObject
public class UniParcSequenceRequest extends UniParcGetByIdRequest {

    @Parameter(description = SEQUENCE_UNIPARC_DESCRIPTION, example = SEQUENCE_UNIPARC_EXAMPLE)
    @NotNull(message = "{search.required}")
    @Pattern(regexp = SEQUENCE_REGEX, message = "{uniparc.sequence.invalid}")
    private String sequence;

    @Parameter(description = FIELDS_UNIPARC_DESCRIPTION, example = FIELDS_UNIPARC_EXAMPLE)
    @ValidReturnFields(uniProtDataType = UniProtDataType.UNIPARC)
    private String fields;
}
