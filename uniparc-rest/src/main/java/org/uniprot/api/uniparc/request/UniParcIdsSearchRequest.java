package org.uniprot.api.uniparc.request;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.PositiveOrZero;

import lombok.Data;

import org.springframework.http.MediaType;
import org.uniprot.api.rest.request.IdsSearchRequest;
import org.uniprot.api.rest.request.QueryFieldMetaReaderImpl;
import org.uniprot.api.rest.request.ReturnFieldMetaReaderImpl;
import org.uniprot.api.rest.request.SortFieldMetaReaderImpl;
import org.uniprot.api.rest.respository.facet.impl.UniParcFacetConfig;
import org.uniprot.api.rest.validation.ValidContentTypes;
import org.uniprot.api.rest.validation.ValidFacets;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.rest.validation.ValidSolrQueryFields;
import org.uniprot.api.rest.validation.ValidSolrQuerySyntax;
import org.uniprot.api.rest.validation.ValidSolrSortFields;
import org.uniprot.api.rest.validation.ValidUniqueIdList;
import org.uniprot.store.config.UniProtDataType;

import uk.ac.ebi.uniprot.openapi.extension.ModelFieldMeta;
import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author sahmad
 * @created 18/03/2021
 */
@Data
public class UniParcIdsSearchRequest implements IdsSearchRequest {
    @NotNull(message = "{search.required}")
    @Parameter(description = "Comma separated list of UniParc ids(upis)")
    @ValidUniqueIdList(uniProtDataType = UniProtDataType.UNIPARC)
    private String upis;

    @ModelFieldMeta(reader = ReturnFieldMetaReaderImpl.class, path = "uniparc-return-fields.json")
    @Parameter(
            hidden = true,
            description = "Comma separated list of fields to be returned in response")
    @ValidReturnFields(uniProtDataType = UniProtDataType.UNIPARC)
    private String fields;

    @Parameter(hidden = true, description = "Name of the facet search")
    @ValidFacets(facetConfig = UniParcFacetConfig.class)
    @ValidContentTypes(contentTypes = {MediaType.APPLICATION_JSON_VALUE})
    private String facets;

    @ModelFieldMeta(reader = QueryFieldMetaReaderImpl.class, path = "uniparc-search-fields.json")
    @Parameter(
            hidden = true,
            description = "Criteria to search the uniparc. It can take any valid solr query.")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(
            uniProtDataType = UniProtDataType.UNIPARC,
            messagePrefix = "search.uniparc")
    protected String query;

    @Parameter(
            hidden = true,
            description =
                    "Adds content disposition attachment to response headers, this way it can be downloaded as a file in the browser.")
    @Pattern(
            regexp = "^(?:true|false)$",
            flags = {Pattern.Flag.CASE_INSENSITIVE},
            message = "{search.uniparc.invalid.download}")
    private String download;

    @Parameter(hidden = true)
    private String cursor;

    @Parameter(
            hidden = true,
            description = "Size of the result. Defaults to number of accessions passed.")
    @PositiveOrZero(message = "{search.positive.or.zero}")
    @Max(value = MAX_RESULTS_SIZE, message = "{search.max.page.size}")
    private Integer size;

    @ModelFieldMeta(reader = SortFieldMetaReaderImpl.class, path = "uniparc-search-fields.json")
    @Parameter(hidden = true, description = "Name of the field to be sorted on")
    @ValidSolrSortFields(uniProtDataType = UniProtDataType.UNIPARC)
    private String sort;

    public String getCommaSeparatedIds() {
        return this.upis;
    }
}
