package org.uniprot.api.uniparc.request;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.uniprot.store.search.field.validator.FieldRegexConstants;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author sahmad
 * @created 14/08/2020
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UniParcGetByUniParcIdRequest extends UniParcGetByIdRequest {
    @Pattern(
            regexp = FieldRegexConstants.UNIPARC_UPI_REGEX,
            flags = {Pattern.Flag.CASE_INSENSITIVE},
            message = "{search.invalid.upi.value}")
    @NotNull(message = "{search.required}")
    @Parameter(description = "UniParc ID (UPI)")
    private String upi;
}
