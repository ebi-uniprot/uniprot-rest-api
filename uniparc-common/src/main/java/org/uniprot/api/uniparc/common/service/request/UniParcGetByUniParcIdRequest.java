package org.uniprot.api.uniparc.common.service.request;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.uniprot.api.rest.request.ReturnFieldMetaReaderImpl;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.search.field.validator.FieldRegexConstants;

import uk.ac.ebi.uniprot.openapi.extension.ModelFieldMeta;
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

    @ModelFieldMeta(reader = ReturnFieldMetaReaderImpl.class, path = "uniparc-return-fields.json")
    @Parameter(description = "Comma separated list of fields to be returned in the response")
    @ValidReturnFields(uniProtDataType = UniProtDataType.UNIPARC)
    private String fields;
}
