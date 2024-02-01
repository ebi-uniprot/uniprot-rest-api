package org.uniprot.api.support.data.common.taxonomy.request;

import static org.uniprot.api.rest.openapi.OpenApiConstants.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.PositiveOrZero;

import lombok.Data;

import org.springframework.http.MediaType;
import org.uniprot.api.rest.openapi.OpenApiConstants;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.validation.*;
import org.uniprot.api.support.data.common.taxonomy.repository.TaxonomyFacetConfig;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.UniProtDataType;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author lgonzales
 * @since 17/09/2020
 */
@Data
public class GetByTaxonIdsRequest implements SearchRequest {

    @Parameter(hidden = true)
    private static final String TAXONOMY_ID_LIST_REGEX = "^\\d+(?:,\\d+)*$";

    @NotNull(message = "{search.required}")
    @Parameter(description = IDS_TAX_DESCRIPTION, example = IDS_TAX_EXAMPLE)
    @Pattern(regexp = TAXONOMY_ID_LIST_REGEX, message = "{search.taxonomy.invalid.list.id}")
    @ValidCommaSeparatedItemsLength(maxLength = 1000)
    private String taxonIds;

    @Parameter(description = FIELDS_TAX_DESCRIPTION, example = FIELDS_TAX_EXAMPLE)
    @ValidReturnFields(uniProtDataType = UniProtDataType.TAXONOMY)
    private String fields;

    @Parameter(hidden = true)
    @ValidFacets(facetConfig = TaxonomyFacetConfig.class)
    @ValidContentTypes(contentTypes = {MediaType.APPLICATION_JSON_VALUE})
    private String facets;

    @Parameter(description = FACET_FILTER_TAX_DESCRIPTION, example = FACET_FILTER_TAX_EXAMPLE)
    @ValidSolrQuerySyntax(message = "{search.taxonomy.ids.invalid.facet.filter}")
    @ValidSolrQueryFacetFields(facetConfig = TaxonomyFacetConfig.class)
    private String facetFilter;

    @Parameter(description = DOWNLOAD_DESCRIPTION)
    @Pattern(
            flags = {Pattern.Flag.CASE_INSENSITIVE},
            regexp = "^true|false$",
            message = "{search.taxonomy.invalid.download}")
    private String download;

    @PositiveOrZero(message = "{search.positive.or.zero}")
    @Max(value = MAX_RESULTS_SIZE, message = "{search.max.page.size}")
    @Parameter(description = IDS_SIZE_TAX_DESCRIPTION)
    private Integer size;

    @Parameter(hidden = true)
    private String cursor;

    @Parameter(hidden = true)
    private String format;

    @Override
    public String getQuery() {
        StringBuilder qb = new StringBuilder();
        qb.append("tax_id:(").append(String.join(" OR ", getTaxonIdsList())).append(")");
        // append the facet filter query in the accession query
        if (Utils.notNullNotEmpty(getFacetFilter())) {
            qb.append(" AND (").append(getFacetFilter()).append(")");
        }
        return qb.toString();
    }

    @Override
    public String getSort() {
        return null;
    }

    public boolean isDownload() {
        return Boolean.parseBoolean(download);
    }

    private List<String> getTaxonIdsList() {
        return Arrays.stream(getTaxonIds().split(",")).collect(Collectors.toList());
    }
}
