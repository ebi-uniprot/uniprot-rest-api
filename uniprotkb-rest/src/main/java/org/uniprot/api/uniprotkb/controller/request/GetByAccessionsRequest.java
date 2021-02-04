package org.uniprot.api.uniprotkb.controller.request;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.constraints.*;

import lombok.Data;

import org.springframework.http.MediaType;
import org.uniprot.api.rest.request.ReturnFieldMetaReaderImpl;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.validation.*;
import org.uniprot.api.uniprotkb.repository.search.impl.UniprotKBFacetConfig;
import org.uniprot.store.config.UniProtDataType;

import uk.ac.ebi.uniprot.openapi.extension.ModelFieldMeta;
import io.swagger.v3.oas.annotations.Parameter;

@Data
public class GetByAccessionsRequest implements SearchRequest {

    @NotNull(message = "{search.required}")
    @Parameter(description = "Comma separated list of accessions")
    @ValidAccessionList
    private String accessions;

    @ModelFieldMeta(reader = ReturnFieldMetaReaderImpl.class, path = "uniprotkb-return-fields.json")
    @Parameter(description = "Comma separated list of fields to be returned in response")
    @ValidReturnFields(uniProtDataType = UniProtDataType.UNIPROTKB)
    private String fields;

    @Parameter(description = "Name of the facet search")
    @ValidFacets(facetConfig = UniprotKBFacetConfig.class)
    @ValidContentTypes(contentTypes = {MediaType.APPLICATION_JSON_VALUE})
    private String facets;

    @Parameter(
            description =
                    "Criteria to filter by facet value. It can any supported valid solr query.")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFacetFields(facetConfig = UniprotKBFacetConfig.class)
    private String facetFilter;

    @Parameter(
            description =
                    "Adds content disposition attachment to response headers, this way it can be downloaded as a file in the browser.")
    @Pattern(
            regexp = "^true|false$",
            flags = {Pattern.Flag.CASE_INSENSITIVE},
            message = "{search.uniprot.invalid.download}")
    private String download;

    @Parameter(hidden = true)
    private String cursor;

    @Parameter(description = "Size of the result. Defaults to number of accessions passed.")
    @PositiveOrZero(message = "{search.positive.or.zero}")
    @Max(value = MAX_RESULTS_SIZE, message = "{search.max.page.size}")
    private Integer size;

    @Override
    public String getQuery() {
        return null;
    }

    @Override
    public String getSort() {
        return null;
    }

    public List<String> getAccessionsList() {
        return Arrays.asList(getAccessions().split(",")).stream()
                .map(String::toUpperCase)
                .collect(Collectors.toList());
    }

    public boolean isDownload() {
        return Boolean.parseBoolean(download);
    }
}
