package org.uniprot.api.support.data.literature.request;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;

import lombok.Data;

import org.uniprot.api.rest.request.QueryFieldMetaReaderImpl;
import org.uniprot.api.rest.request.ReturnFieldMetaReaderImpl;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.request.SortFieldMetaReaderImpl;
import org.uniprot.api.rest.validation.*;
import org.uniprot.api.support.data.literature.repository.LiteratureFacetConfig;
import org.uniprot.store.config.UniProtDataType;

import uk.ac.ebi.uniprot.openapi.extension.ModelFieldMeta;
import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author lgonzales
 * @since 2019-07-04
 */
@Data
public class LiteratureRequest implements SearchRequest {

    @ModelFieldMeta(reader = QueryFieldMetaReaderImpl.class, path = "literature-search-fields.json")
    @Parameter(
            description =
                    "Criteria to search literature publications. It can take any valid solr query.")
    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(
            uniProtDataType = UniProtDataType.LITERATURE,
            messagePrefix = "search.literature")
    private String query;

    @ModelFieldMeta(reader = SortFieldMetaReaderImpl.class, path = "literature-search-fields.json")
    @Parameter(description = "Name of the field to be sorted on")
    @ValidSolrSortFields(uniProtDataType = UniProtDataType.LITERATURE)
    private String sort;

    @Parameter(hidden = true)
    private String cursor;

    @ModelFieldMeta(
            reader = ReturnFieldMetaReaderImpl.class,
            path = "literature-return-fields.json")
    @Parameter(description = "Comma separated list of fields to be returned in response")
    @ValidReturnFields(uniProtDataType = UniProtDataType.LITERATURE)
    private String fields;

    @Parameter(description = "Size of the result. Defaults to 25")
    @PositiveOrZero(message = "{search.positive.or.zero}")
    @Max(value = MAX_RESULTS_SIZE, message = "{search.max.page.size}")
    private Integer size;

    @Parameter(description = "Comma separated list of facets to search")
    @ValidFacets(facetConfig = LiteratureFacetConfig.class)
    private String facets;
}
