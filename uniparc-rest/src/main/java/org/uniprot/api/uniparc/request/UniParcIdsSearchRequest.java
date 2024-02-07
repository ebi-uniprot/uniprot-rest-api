package org.uniprot.api.uniparc.request;

import static org.uniprot.api.rest.openapi.OpenApiConstants.*;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.PositiveOrZero;

import lombok.Data;

import org.springdoc.api.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.uniprot.api.rest.request.IdsSearchRequest;
import org.uniprot.api.rest.respository.facet.impl.UniParcFacetConfig;
import org.uniprot.api.rest.validation.ValidContentTypes;
import org.uniprot.api.rest.validation.ValidFacets;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.rest.validation.ValidSolrQueryFields;
import org.uniprot.api.rest.validation.ValidSolrQuerySyntax;
import org.uniprot.api.rest.validation.ValidSolrSortFields;
import org.uniprot.api.rest.validation.ValidUniqueIdList;
import org.uniprot.store.config.UniProtDataType;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author sahmad
 * @created 18/03/2021
 */
@Data
@ParameterObject
public class UniParcIdsSearchRequest implements IdsSearchRequest {
    @NotNull(message = "{search.required}")
    @Parameter(description = IDS_UNIPARC_DESCRIPTION, example = IDS_UNIPARC_EXAMPLE)
    @ValidUniqueIdList(uniProtDataType = UniProtDataType.UNIPARC)
    private String upis;

    @Parameter(description = FIELDS_UNIPARC_DESCRIPTION, example = FIELDS_UNIPARC_EXAMPLE)
    @ValidReturnFields(uniProtDataType = UniProtDataType.UNIPARC)
    private String fields;

    @Parameter(hidden = true)
    @ValidFacets(facetConfig = UniParcFacetConfig.class)
    @ValidContentTypes(contentTypes = {MediaType.APPLICATION_JSON_VALUE})
    private String facets;

    @Parameter(description = QUERY_UNIPARC_DESCRIPTION, example = QUERY_UNIPARC_EXAMPLE)
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(
            uniProtDataType = UniProtDataType.UNIPARC,
            messagePrefix = "search.uniparc")
    protected String query;

    @Parameter(description = DOWNLOAD_DESCRIPTION)
    @Pattern(
            regexp = "^(?:true|false)$",
            flags = {Pattern.Flag.CASE_INSENSITIVE},
            message = "{search.uniparc.invalid.download}")
    private String download;

    @Parameter(hidden = true)
    private String cursor;

    @Parameter(description = SIZE_IDS_UNIPARC_DESCRIPTION)
    @PositiveOrZero(message = "{search.positive.or.zero}")
    @Max(value = MAX_RESULTS_SIZE, message = "{search.max.page.size}")
    private Integer size;

    @Parameter(description = SORT_UNIPARC_DESCRIPTION, example = SORT_UNIPARC_EXAMPLE)
    @ValidSolrSortFields(uniProtDataType = UniProtDataType.UNIPARC)
    private String sort;

    @Parameter(hidden = true)
    private String format;

    public String getCommaSeparatedIds() {
        return this.upis;
    }
}
