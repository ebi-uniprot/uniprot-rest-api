package org.uniprot.api.uniprotkb.common.service.uniprotkb.request;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.PositiveOrZero;

import lombok.Data;

import org.springframework.http.MediaType;
import org.uniprot.api.rest.request.*;
import org.uniprot.api.rest.respository.facet.impl.UniProtKBFacetConfig;
import org.uniprot.api.rest.validation.*;
import org.uniprot.store.config.UniProtDataType;

import uk.ac.ebi.uniprot.openapi.extension.ModelFieldMeta;
import io.swagger.v3.oas.annotations.Parameter;

@Data
@ValidGetByIdsRequest
public class UniProtKBIdsSearchRequest implements IdsSearchRequest {

    @NotNull(message = "{search.required}")
    @Parameter(description = "Comma separated list of accessions")
    @ValidUniqueIdList(uniProtDataType = UniProtDataType.UNIPROTKB)
    private String accessions;

    @ModelFieldMeta(reader = ReturnFieldMetaReaderImpl.class, path = "uniprotkb-return-fields.json")
    @Parameter(description = "Comma separated list of fields to be returned in response")
    @ValidReturnFields(uniProtDataType = UniProtDataType.UNIPROTKB)
    private String fields;

    @Parameter(description = "Name of the facet search")
    @ValidFacets(facetConfig = UniProtKBFacetConfig.class)
    @ValidContentTypes(contentTypes = {MediaType.APPLICATION_JSON_VALUE})
    private String facets;

    @ModelFieldMeta(reader = QueryFieldMetaReaderImpl.class, path = "uniprotkb-search-fields.json")
    @Parameter(description = "Criteria to search the proteins. It can take any valid solr query.")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(
            uniProtDataType = UniProtDataType.UNIPROTKB,
            messagePrefix = "search.uniprot")
    private String query;

    @Parameter(
            description =
                    "Adds content disposition attachment to response headers, this way it can be downloaded as a file in the browser.")
    @Pattern(
            regexp = "^(?:true|false)$",
            flags = {Pattern.Flag.CASE_INSENSITIVE},
            message = "{search.uniprot.invalid.download}")
    private String download;

    @Parameter(hidden = true)
    private String cursor;

    @Parameter(description = "Size of the result. Defaults to number of accessions passed.")
    @PositiveOrZero(message = "{search.positive.or.zero}")
    @Max(value = SearchRequest.MAX_RESULTS_SIZE, message = "{search.max.page.size}")
    private Integer size;

    @ModelFieldMeta(reader = SortFieldMetaReaderImpl.class, path = "uniprotkb-search-fields.json")
    @Parameter(description = "Name of the field to be sorted on")
    @ValidSolrSortFields(uniProtDataType = UniProtDataType.UNIPROTKB)
    private String sort;

    @Parameter(hidden = true)
    private String format;

    public String getCommaSeparatedIds() {
        return this.accessions;
    }
}
