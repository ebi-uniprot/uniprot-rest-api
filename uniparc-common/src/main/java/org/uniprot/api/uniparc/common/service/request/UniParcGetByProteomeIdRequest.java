package org.uniprot.api.uniparc.common.service.request;

import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.PositiveOrZero;

import org.springdoc.api.annotations.ParameterObject;
import org.uniprot.api.rest.request.SearchRequest;
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
public class UniParcGetByProteomeIdRequest extends UniParcGetByIdRequest implements SearchRequest {
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

    @Parameter(description = SIZE_DESCRIPTION, example = SIZE_EXAMPLE)
    @PositiveOrZero(message = "{search.positive.or.zero}")
    @Max(value = SearchRequest.MAX_RESULTS_SIZE, message = "{search.max.page.size}")
    private Integer size;

    @Parameter(hidden = true)
    private String fields;

    @Parameter(hidden = true)
    private String cursor;

    @Parameter(hidden = true)
    private String facets;

    @Override
    public String getSort() {
        return null;
    }

    @Override
    public String getQuery() {
        return PROTEOME_ID_STR + ":" + this.upId;
    }
}
