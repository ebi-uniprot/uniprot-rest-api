package org.uniprot.api.uniref.request;

import javax.validation.constraints.Pattern;

import lombok.Data;

import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.search.field.validator.FieldRegexConstants;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author lgonzales
 * @since 21/07/2020
 */
@Data
public class UniRefIdRequest {

    @Parameter(description = "Unique identifier for the UniRef cluster")
    @Pattern(
            regexp = FieldRegexConstants.UNIREF_CLUSTER_ID_REGEX,
            flags = {Pattern.Flag.CASE_INSENSITIVE},
            message = "{search.invalid.id.value}")
    private String id;

    @ValidReturnFields(uniProtDataType = UniProtDataType.UNIREF)
    @Parameter(description = "Comma separated list of fields to be returned in response")
    private String fields;
}
