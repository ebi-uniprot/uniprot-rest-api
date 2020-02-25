package org.uniprot.api.uniparc.request;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import lombok.Data;

import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.validation.*;
import org.uniprot.api.uniparc.repository.UniParcFacetConfig;
import org.uniprot.store.config.searchfield.factory.UniProtDataType;
import org.uniprot.store.search.field.UniParcField;
import org.uniprot.store.search.field.UniParcResultFields;

import com.google.common.base.Strings;

/**
 * @author jluo
 * @date: 20 Jun 2019
 */
@Data
public class UniParcRequest implements SearchRequest {
    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(
            uniProtDataType = UniProtDataType.uniparc,
            messagePrefix = "search.uniparc")
    private String query;

    @ValidSolrSortFields(uniProtDataType = UniProtDataType.uniparc)
    private String sort;

    private String cursor;

    @ValidReturnFields(fieldValidatorClazz = UniParcResultFields.class)
    private String fields;

    @ValidFacets(facetConfig = UniParcFacetConfig.class)
    private String facets;

    @Positive(message = "{search.positive}")
    private Integer size;

    public static final String DEFAULT_FIELDS =
            "upi,organism,accession,first_seen,last_seen,length";

    @Override
    public String getFields() {
        if (Strings.isNullOrEmpty(fields)) {
            fields = DEFAULT_FIELDS;
        } else if (!fields.contains(UniParcField.Return.upi.name())) {
            String temp = "upi," + fields;
            this.fields = temp;
        }
        return fields;
    }
}
