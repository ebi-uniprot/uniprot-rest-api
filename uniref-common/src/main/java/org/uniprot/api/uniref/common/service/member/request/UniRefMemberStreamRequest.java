package org.uniprot.api.uniref.common.service.member.request;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springdoc.api.annotations.ParameterObject;
import org.uniprot.api.rest.request.StreamRequest;
import org.uniprot.api.uniref.common.service.light.request.UniRefBasicRequest;

import javax.validation.constraints.Pattern;

import static org.uniprot.api.rest.openapi.OpenAPIConstants.DOWNLOAD_DESCRIPTION;

/**
 * @author lgonzales
 * @since 18/06/2020
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ParameterObject
public class UniRefMemberStreamRequest extends UniRefMemberRequest implements StreamRequest {

    @Parameter(description = DOWNLOAD_DESCRIPTION)
    @Pattern(
            regexp = "^true$|^false$",
            flags = {Pattern.Flag.CASE_INSENSITIVE},
            message = "{search.uniref.invalid.download}")
    private String download;

    private boolean isLargeSolrStreamRestricted = true;
}
