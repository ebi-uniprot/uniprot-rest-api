package org.uniprot.api.proteome.request;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import lombok.Data;

import org.uniprot.api.proteome.repository.GeneCentricFacetConfig;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.validation.*;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.search.field.GeneCentricField;

import com.google.common.base.Strings;

/**
 * @author jluo
 * @date: 17 May 2019
 */
@Data
public class GeneCentricRequest implements SearchRequest {
    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(
            uniProtDataType = UniProtDataType.GENECENTRIC,
            messagePrefix = "search.genecentric")
    private String query;

    @ValidSolrSortFields(uniProtDataType = UniProtDataType.GENECENTRIC)
    private String sort;

    private String cursor;

    @ValidReturnFields(uniProtDataType = UniProtDataType.GENECENTRIC)
    private String fields;

    @ValidFacets(facetConfig = GeneCentricFacetConfig.class)
    private String facets;

    @Positive(message = "{search.positive}")
    private Integer size;

    private static final String DEFAULT_FIELDS = "accession_id";

    @Override
    public String getFields() {
        if (Strings.isNullOrEmpty(fields)) {
            fields = DEFAULT_FIELDS;
        } else if (!fields.contains(GeneCentricField.ResultFields.accession_id.name())) {
            String temp = "accession_id," + fields;
            this.fields = temp;
        }
        return fields;
    }
}
