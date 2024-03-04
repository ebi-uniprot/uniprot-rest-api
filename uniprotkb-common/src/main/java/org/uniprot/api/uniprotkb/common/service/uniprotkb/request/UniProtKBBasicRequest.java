package org.uniprot.api.uniprotkb.common.service.uniprotkb.request;

import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import lombok.Data;

import org.uniprot.api.rest.validation.*;
import org.uniprot.store.config.UniProtDataType;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author lgonzales
 * @since 18/06/2020
 */
@Data
public class UniProtKBBasicRequest {

    @Parameter(description = QUERY_UNIPROTKB_SEARCH_DESCRIPTION, example = QUERY_UNIPROTKB_EXAMPLE)
    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(
            uniProtDataType = UniProtDataType.UNIPROTKB,
            messagePrefix = "search.uniprot")
    private String query;

    @Parameter(description = FIELDS_UNIPROTKB_DESCRIPTION, example = FIELDS_UNIPROTKB_EXAMPLE)
    @ValidReturnFields(uniProtDataType = UniProtDataType.UNIPROTKB)
    @ValidTSVAndXLSFormatOnlyFields(fieldPattern = "xref_.*_full")
    private String fields;

    @Parameter(description = SORT_UNIPROTKB_DESCRIPTION, example = SORT_UNIPROTKB_EXAMPLE)
    @ValidSolrSortFields(uniProtDataType = UniProtDataType.UNIPROTKB)
    private String sort;

    @Parameter(description = INCLUDE_ISOFORM_DESCRIPTION)
    @Pattern(
            regexp = "true|false",
            flags = {Pattern.Flag.CASE_INSENSITIVE},
            message = "{search.invalid.includeIsoform}")
    private String includeIsoform;

    @Parameter(hidden = true)
    private String format;

    public boolean isIncludeIsoform() {
        return Boolean.parseBoolean(includeIsoform);
    }
}
