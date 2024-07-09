package org.uniprot.api.idmapping.common.request.uniref;

import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;

import javax.validation.constraints.Pattern;

import org.uniprot.api.idmapping.common.request.IdMappingPageRequest;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.rest.validation.ValidSolrQueryFields;
import org.uniprot.api.rest.validation.ValidSolrQuerySyntax;
import org.uniprot.api.rest.validation.ValidSolrSortFields;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.UniProtDataType;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author lgonzales
 * @since 25/02/2021
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UniRefIdMappingBasicRequest extends IdMappingPageRequest {

    @Parameter(description = QUERY_UNIREF_DESCRIPTION, example = QUERY_UNIREF_EXAMPLE)
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(uniProtDataType = UniProtDataType.UNIREF, messagePrefix = "search.uniref")
    private String query;

    @Parameter(description = FIELDS_UNIREF_DESCRIPTION, example = FIELDS_UNIREF_EXAMPLE)
    @ValidReturnFields(uniProtDataType = UniProtDataType.UNIREF)
    private String fields;

    @Parameter(description = SORT_UNIREF_DESCRIPTION, example = SORT_UNIREF_EXAMPLE)
    @ValidSolrSortFields(uniProtDataType = UniProtDataType.UNIREF)
    private String sort;

    @Parameter(description = COMPLETE_UNIREF_DESCRIPTION)
    @Pattern(
            regexp = "^(?:true|false)$",
            flags = {Pattern.Flag.CASE_INSENSITIVE},
            message = "{search.uniref.invalid.complete.value}")
    private String complete;

    @Parameter(hidden = true)
    private String format;

    public boolean isComplete() {
        boolean result = false;
        if (Utils.notNullNotEmpty(complete)) {
            result = Boolean.parseBoolean(complete);
        }
        return result;
    }
}
