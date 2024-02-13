package org.uniprot.api.idmapping.common.request.uniprotkb;

import static org.uniprot.api.rest.openapi.OpenApiConstants.*;

import javax.validation.constraints.Pattern;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.uniprot.api.idmapping.common.request.IdMappingPageRequest;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.validation.*;
import org.uniprot.store.config.UniProtDataType;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author sahmad
 * @created 18/02/2021
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UniProtKBIdMappingBasicRequest extends IdMappingPageRequest {

    @Parameter(description = QUERY_UNIPROTKB_SEARCH_DESCRIPTION, example = QUERY_UNIPROTKB_EXAMPLE)
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(
            uniProtDataType = UniProtDataType.UNIPROTKB,
            messagePrefix = "search.uniprot")
    private String query;

    @Parameter(description = FIELDS_UNIPROTKB_DESCRIPTION, example = FIELDS_UNIPROTKB_EXAMPLE)
    @ValidReturnFields(uniProtDataType = UniProtDataType.UNIPROTKB)
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

    @Parameter(description = SUB_SEQUENCE_DESCRIPTION)
    @Pattern(
            regexp = "true|false",
            flags = {Pattern.Flag.CASE_INSENSITIVE},
            message = "{search.invalid.subsequence}")
    @ValidContentTypes(
            contentTypes = {UniProtMediaType.FASTA_MEDIA_TYPE_VALUE},
            message = "{search.invalid.contentType.subsequence}")
    private String subsequence;

    @Parameter(hidden = true)
    private String format;

    public boolean isSubSequence() {
        return Boolean.parseBoolean(subsequence);
    }

    public boolean isIncludeIsoform() {
        return Boolean.parseBoolean(includeIsoform);
    }
}
