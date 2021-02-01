package org.uniprot.api.support.data.taxonomy.request;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.constraints.*;

import lombok.Data;

import org.springframework.http.MediaType;
import org.uniprot.api.rest.request.ReturnFieldMetaReaderImpl;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.validation.*;
import org.uniprot.api.support.data.taxonomy.repository.TaxonomyFacetConfig;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.UniProtDataType;

import uk.ac.ebi.uniprot.openapi.extension.ModelFieldMeta;
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
    @Parameter(description = "Comma separated list of taxonIds")
    @Pattern(regexp = TAXONOMY_ID_LIST_REGEX, message = "{search.taxonomy.invalid.list.id}")
    @ValidCommaSeparatedItemsLength(maxLength = 1000)
    private String taxonIds;

    @ModelFieldMeta(reader = ReturnFieldMetaReaderImpl.class, path = "taxonomy-return-fields.json")
    @Parameter(description = "Comma separated list of fields to be returned in response")
    @ValidReturnFields(uniProtDataType = UniProtDataType.TAXONOMY)
    private String fields;

    @Parameter(description = "Name of the facet search")
    @ValidFacets(facetConfig = TaxonomyFacetConfig.class)
    @ValidContentTypes(contentTypes = {MediaType.APPLICATION_JSON_VALUE})
    private String facets;

    @Parameter(
            description =
                    "Criteria to filter by facet value. It can any supported valid solr query.")
    @ValidSolrQuerySyntax(message = "{search.taxonomy.ids.invalid.facet.filter}")
    @ValidSolrQueryFacetFields(facetConfig = TaxonomyFacetConfig.class)
    private String facetFilter;

    @Parameter(
            description =
                    "Adds content disposition attachment to response headers, this way it can be downloaded as a file in the browser.")
    @Pattern(
            flags = {Pattern.Flag.CASE_INSENSITIVE},
            regexp = "^true|false$",
            message = "{search.taxonomy.invalid.download}")
    private String download;

    @PositiveOrZero(message = "{search.positive.or.zero}")
    @Max(value = MAX_RESULTS_SIZE, message = "{search.max.page.size}")
    @Parameter(description = "Size of the result. Defaults to number of ids passed.")
    private Integer size;

    @Parameter(hidden = true)
    private String cursor;

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
