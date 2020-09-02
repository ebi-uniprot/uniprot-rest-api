package org.uniprot.api.uniref.request;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;

import lombok.Data;

import org.uniprot.api.rest.request.ReturnFieldMetaReaderImpl;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.rest.validation.ValidSolrQueryFacetFields;
import org.uniprot.api.rest.validation.ValidSolrQuerySyntax;
import org.uniprot.api.uniref.repository.store.UniRefEntryFacetConfig;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.UniProtDataType;

import uk.ac.ebi.uniprot.openapi.extension.ModelFieldMeta;
import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author lgonzales
 * @since 21/07/2020
 */
@Data
public class UniRefIdRequest {

    @ModelFieldMeta(reader = ReturnFieldMetaReaderImpl.class, path = "uniref-return-fields.json")
    @ValidReturnFields(uniProtDataType = UniProtDataType.UNIREF)
    @Parameter(description = "Comma separated list of fields to be returned in response")
    private String fields;

    @Parameter(description = "Facet filter query for UniRef Cluster Members")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFacetFields(facetConfig = UniRefEntryFacetConfig.class)
    private String filter;

    @Parameter(
            description =
                    "Flag to include all member ids and organisms, or not. By default, it returns a maximum of 10 member ids and organisms")
    @Pattern(
            regexp = "true|false",
            flags = {Pattern.Flag.CASE_INSENSITIVE},
            message = "{search.uniref.invalid.complete.value}")
    private String complete;

    @Parameter(hidden = true)
    private String cursor;

    @Parameter(description = "Size of the result. Defaults to 25")
    @Positive(message = "{search.positive}")
    private Integer size;

    public boolean isComplete() {
        boolean result = false;
        if (Utils.notNullNotEmpty(complete)) {
            result = Boolean.parseBoolean(complete);
        }
        return result;
    }
}
