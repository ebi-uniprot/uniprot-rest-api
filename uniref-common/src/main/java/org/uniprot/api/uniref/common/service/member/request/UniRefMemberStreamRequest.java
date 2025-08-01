package org.uniprot.api.uniref.common.service.member.request;

import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.springdoc.api.annotations.ParameterObject;
import org.uniprot.api.rest.request.StreamRequest;
import org.uniprot.store.search.field.validator.FieldRegexConstants;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

/**
 * @author lgonzales
 * @since 05/01/2021
 */
@Data
@ParameterObject
public class UniRefMemberStreamRequest implements StreamRequest {

    @Parameter(description = ID_UNIREF_DESCRIPTION, example = ID_UNIREF_EXAMPLE)
    @Pattern(
            regexp = FieldRegexConstants.UNIREF_CLUSTER_ID_REGEX,
            flags = {Pattern.Flag.CASE_INSENSITIVE},
            message = "{search.invalid.id.value}")
    @NotNull(message = "{search.required}")
    private String id;

    @Parameter(description = DOWNLOAD_DESCRIPTION)
    @Pattern(
            regexp = "^true$|^false$",
            flags = {Pattern.Flag.CASE_INSENSITIVE},
            message = "{search.uniref.invalid.download}")
    private String download;

    @Parameter(hidden = true)
    private String format;

    @Parameter(hidden = true)
    private String query;

    @Parameter(hidden = true)
    private String fields;

    @Parameter(hidden = true)
    private String sort;

    private boolean isLargeSolrStreamRestricted = true;
}
