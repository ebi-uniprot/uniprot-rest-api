package org.uniprot.api.uniprotkb.common.service.uniprotkb.request;

import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.PositiveOrZero;

import org.springdoc.api.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.uniprot.api.rest.request.*;
import org.uniprot.api.rest.respository.facet.impl.UniProtKBFacetConfig;
import org.uniprot.api.rest.validation.*;
import org.uniprot.store.config.UniProtDataType;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

@Data
@ParameterObject
@ValidGetByIdsRequest
public class UniProtKBIdsSearchRequest implements IdsSearchRequest {

    @NotNull(message = "{search.required}")
    @Parameter(
            description = ACCESSIONS_UNIPROTKB_DESCRIPTION,
            example = ACCESSIONS_UNIPROTKB_EXAMPLE)
    @ValidUniqueIdList(uniProtDataType = UniProtDataType.UNIPROTKB)
    private String accessions;

    @Parameter(description = FIELDS_UNIPROTKB_DESCRIPTION, example = FIELDS_UNIPROTKB_EXAMPLE)
    @ValidReturnFields(uniProtDataType = UniProtDataType.UNIPROTKB)
    private String fields;

    @Parameter(hidden = true)
    @ValidFacets(facetConfig = UniProtKBFacetConfig.class)
    @ValidContentTypes(contentTypes = {MediaType.APPLICATION_JSON_VALUE})
    private String facets;

    @Parameter(description = QUERY_UNIPROTKB_ID_DESCRIPTION, example = QUERY_UNIPROTKB_EXAMPLE)
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(
            uniProtDataType = UniProtDataType.UNIPROTKB,
            messagePrefix = "search.uniprot")
    private String query;

    @Parameter(description = DOWNLOAD_DESCRIPTION)
    @Pattern(
            regexp = "^(?:true|false)$",
            flags = {Pattern.Flag.CASE_INSENSITIVE},
            message = "{search.uniprot.invalid.download}")
    private String download;

    @Parameter(hidden = true)
    private String cursor;

    @Parameter(description = SIZE_UNIPROTKB_ID_DESCRIPTION)
    @PositiveOrZero(message = "{search.positive.or.zero}")
    @Max(value = SearchRequest.MAX_IDS_RESULTS_SIZE, message = "{search.max.page.size}")
    private Integer size;

    @Parameter(description = SORT_UNIPROTKB_ID_DESCRIPTION, example = SORT_UNIPROTKB_EXAMPLE)
    @ValidSolrSortFields(uniProtDataType = UniProtDataType.UNIPROTKB)
    private String sort;

    @Parameter(hidden = true)
    private String format;

    public String getCommaSeparatedIds() {
        return this.accessions;
    }
}
