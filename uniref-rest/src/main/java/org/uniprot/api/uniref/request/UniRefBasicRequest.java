package org.uniprot.api.uniref.request;

import static org.uniprot.api.rest.openapi.OpenApiConstants.*;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import lombok.Data;

import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.rest.validation.ValidSolrQueryFields;
import org.uniprot.api.rest.validation.ValidSolrQuerySyntax;
import org.uniprot.api.rest.validation.ValidSolrSortFields;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.UniProtDataType;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author lgonzales
 * @since 18/06/2020
 */
@Data
public class UniRefBasicRequest {

    @Parameter(description = QUERY_UNIREF_DESCRIPTION, example = QUERY_UNIREF_EXAMPLE)
    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(uniProtDataType = UniProtDataType.UNIREF, messagePrefix = "search.uniref")
    private String query;

    @Parameter(description = SORT_UNIREF_DESCRIPTION)
    @ValidSolrSortFields(uniProtDataType = UniProtDataType.UNIREF)
    private String sort;

    @Parameter(description = FIELDS_UNIREF_DESCRIPTION, example = FIELDS_UNIREF_EXAMPLE)
    @ValidReturnFields(uniProtDataType = UniProtDataType.UNIREF)
    private String fields;

    @Parameter(description = COMPLETE_UNIREF_DESCRIPTION)
    @Pattern(
            regexp = "true|false",
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
