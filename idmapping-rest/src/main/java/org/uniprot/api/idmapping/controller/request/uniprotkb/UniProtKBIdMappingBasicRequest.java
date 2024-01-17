package org.uniprot.api.idmapping.controller.request.uniprotkb;

import javax.validation.constraints.Pattern;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.uniprot.api.idmapping.controller.request.IdMappingPageRequest;
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

    @Parameter(description = "Criteria to search the proteins. It can take any valid lucene query.")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(
            uniProtDataType = UniProtDataType.UNIPROTKB,
            messagePrefix = "search.uniprot")
    private String query;

    @Parameter(description = "Comma separated list of fields to be returned in response")
    @ValidReturnFields(uniProtDataType = UniProtDataType.UNIPROTKB)
    private String fields;

    @Parameter(description = "Name of the field to be sorted on")
    @ValidSolrSortFields(uniProtDataType = UniProtDataType.UNIPROTKB)
    private String sort;

    @Parameter(description = "Flag to include Isoform or not")
    @Pattern(
            regexp = "true|false",
            flags = {Pattern.Flag.CASE_INSENSITIVE},
            message = "{search.invalid.includeIsoform}")
    private String includeIsoform;

    @Parameter(description = "Flag to write subsequences. Only accepted in fasta format")
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
