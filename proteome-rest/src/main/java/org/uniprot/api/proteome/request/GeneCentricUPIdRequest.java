package org.uniprot.api.proteome.request;

import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;
import static org.uniprot.api.rest.request.SearchRequest.MAX_RESULTS_SIZE;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.PositiveOrZero;

import org.springdoc.api.annotations.ParameterObject;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.search.field.validator.FieldRegexConstants;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

/**
 * @author lgonzales
 * @since 31/10/2020
 */
@Data
@ParameterObject
public class GeneCentricUPIdRequest {

    @Parameter(description = UPID_PROTEOME_DESCRIPTION, example = UPID_PROTEOME_EXAMPLE)
    @NotNull(message = "{search.required}")
    @Pattern(
            regexp = FieldRegexConstants.PROTEOME_ID_REGEX,
            flags = {Pattern.Flag.CASE_INSENSITIVE},
            message = "{search.invalid.upid.value}")
    private String upid;

    @Parameter(description = FIELDS_GENECENTRIC_DESCRIPTION, example = FIELDS_GENECENTRIC_EXAMPLE)
    @ValidReturnFields(uniProtDataType = UniProtDataType.GENECENTRIC)
    private String fields;

    @Parameter(hidden = true)
    private String cursor;

    @Parameter(description = SIZE_DESCRIPTION)
    @PositiveOrZero(message = "{search.positive.or.zero}")
    @Max(value = MAX_RESULTS_SIZE, message = "{search.max.page.size}")
    private Integer size;
}
