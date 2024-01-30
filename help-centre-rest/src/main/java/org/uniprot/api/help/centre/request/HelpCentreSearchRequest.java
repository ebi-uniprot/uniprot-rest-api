package org.uniprot.api.help.centre.request;

import static org.uniprot.api.rest.openapi.OpenApiConstants.*;

import java.util.stream.Collectors;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;

import lombok.Data;

import org.uniprot.api.help.centre.repository.HelpCentreFacetConfig;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.validation.*;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;
import org.uniprot.store.config.returnfield.model.ReturnField;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author lgonzales
 * @since 07/07/2021
 */
@Data
public class HelpCentreSearchRequest implements SearchRequest {

    @Parameter(hidden = true)
    private static final String fieldsWithoutContent =
            ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.HELP).getReturnFields()
                    .stream()
                    .map(ReturnField::getName)
                    .filter(fieldName -> !fieldName.equals("content"))
                    .collect(Collectors.joining(","));

    @Parameter(description = QUERY_DESCRIPTION)
    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(
            uniProtDataType = UniProtDataType.HELP,
            messagePrefix = "search.helpcentre")
    private String query;

    @Parameter(description = SORT_DESCRIPTION)
    @ValidSolrSortFields(uniProtDataType = UniProtDataType.HELP)
    private String sort;

    @Parameter(description = FIELDS_DESCRIPTION)
    @ValidReturnFields(uniProtDataType = UniProtDataType.HELP)
    private String fields;

    @Parameter(hidden = true)
    @ValidFacets(facetConfig = HelpCentreFacetConfig.class)
    private String facets;

    @Parameter(description = SIZE_DESCRIPTION)
    @PositiveOrZero(message = "{search.positive.or.zero}")
    @Max(value = MAX_RESULTS_SIZE, message = "{search.max.page.size}")
    private Integer size;

    @Parameter(hidden = true)
    private String cursor;

    @Parameter(hidden = true)
    private String type;

    @Parameter(hidden = true)
    private String format;

    public String getFields() {
        String result = fields;
        if (Utils.nullOrEmpty(result)) {
            result = fieldsWithoutContent;
        }
        return result;
    }
}
