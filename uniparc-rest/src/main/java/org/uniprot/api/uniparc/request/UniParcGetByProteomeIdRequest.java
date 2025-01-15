package org.uniprot.api.uniparc.request;

import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.springdoc.api.annotations.ParameterObject;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.uniparc.common.service.request.UniParcGetByIdPageSearchRequest;
import org.uniprot.store.search.field.validator.FieldRegexConstants;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author sahmad
 * @created 13/08/2020
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ParameterObject
public class UniParcGetByProteomeIdRequest extends UniParcGetByIdPageSearchRequest
        implements SearchRequest {
    @Parameter(hidden = true)
    private static final String PROTEOME_ID_STR = "proteome";

    @Parameter(
            description = PROTEOME_UPID_UNIPARC_DESCRIPTION,
            example = PROTEOME_UPID_UNIPARC_EXAMPLE)
    @NotNull(message = "{search.required}")
    @Pattern(
            regexp = FieldRegexConstants.PROTEOME_ID_REGEX,
            flags = {Pattern.Flag.CASE_INSENSITIVE},
            message = "{search.invalid.upid.value}")
    private String upId;

    @Override
    public String getQuery() {
        return PROTEOME_ID_STR + ":" + this.upId;
    }
}
