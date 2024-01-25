package org.uniprot.api.uniprotkb.controller.request;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.PositiveOrZero;

import lombok.Data;

import org.springdoc.api.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.uniprot.api.rest.request.IdsSearchRequest;
import org.uniprot.api.rest.respository.facet.impl.UniProtKBFacetConfig;
import org.uniprot.api.rest.validation.*;
import org.uniprot.store.config.UniProtDataType;

import io.swagger.v3.oas.annotations.Parameter;

@Data
@ParameterObject
public class UniProtKBIdsSearchRequest implements IdsSearchRequest {

    @NotNull(message = "{search.required}")
    @Parameter(description = "List of UniProtKB accessions, separated by commas.", example = "P05067,P12345,P20802")
    @ValidUniqueIdList(uniProtDataType = UniProtDataType.UNIPROTKB)
    private String accessions;

    @Parameter(description = "List of fields to be returned, separated by commas. <a href='https://rest.uniprot.org/configure/uniprotkb/result-fields'>List of valid fields</a>", example = "accession,protein_name,gene_names,organism_name")
    @ValidReturnFields(uniProtDataType = UniProtDataType.UNIPROTKB)
    private String fields;

    @Parameter(description = "List of facets to be applied, separated by commas. <a href='https://rest.uniprot.org/configure/uniprotkb/facets'>List of valid facets</a>", example = "reviewed,model_organism")
    @ValidFacets(facetConfig = UniProtKBFacetConfig.class)
    @ValidContentTypes(contentTypes = {MediaType.APPLICATION_JSON_VALUE})
    private String facets;

    @Parameter(description = "Criteria to search within the accessions. Advanced queries can be built with parentheses and conditionals such as AND/OR/NOT.  <a href='https://rest.uniprot.org/configure/uniprotkb/search-fields'>List of valid search fields</a>", example = "insulin AND reviewed:true")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(
            uniProtDataType = UniProtDataType.UNIPROTKB,
            messagePrefix = "search.uniprot")
    private String query;

    @Parameter(
            description =
                    "Default: <tt>false</tt>. Use <tt>true</tt> to download as a file.")
    @Pattern(
            regexp = "^(?:true|false)$",
            flags = {Pattern.Flag.CASE_INSENSITIVE},
            message = "{search.uniprot.invalid.download}")
    private String download;

    @Parameter(hidden = true)
    private String cursor;

    @Parameter(description = "Pagination size. Defaults to number of accessions passed (Single page).")
    @PositiveOrZero(message = "{search.positive.or.zero}")
    @Max(value = MAX_RESULTS_SIZE, message = "{search.max.page.size}")
    private Integer size;

    @Parameter(description = "Name of the field to be sorted on. Defaults to order of accessions passed. <a href='https://rest.uniprot.org/configure/uniprotkb/sort'>List of valid sort fields</a>", example = "accession desc")
    @ValidSolrSortFields(uniProtDataType = UniProtDataType.UNIPROTKB)
    private String sort;

    @Parameter(hidden = true)
    private String format;

    public String getCommaSeparatedIds() {
        return this.accessions;
    }
}
