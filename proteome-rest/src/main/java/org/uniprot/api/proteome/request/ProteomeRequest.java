package org.uniprot.api.proteome.request;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import lombok.Data;

import org.uniprot.api.proteome.repository.ProteomeFacetConfig;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.validation.*;
import org.uniprot.store.config.searchfield.factory.UniProtDataType;
import org.uniprot.store.search.field.ProteomeField;
import org.uniprot.store.search.field.ProteomeResultFields;

import com.google.common.base.Strings;

/**
 * @author jluo
 * @date: 26 Apr 2019
 */
@Data
public class ProteomeRequest implements SearchRequest {
    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(
            uniProtDataType = UniProtDataType.PROTEOME,
            messagePrefix = "search.proteome")
    private String query;

    @ValidSolrSortFields(uniProtDataType = UniProtDataType.PROTEOME)
    private String sort;

    private String cursor;

    @ValidReturnFields(fieldValidatorClazz = ProteomeResultFields.class)
    private String fields;

    @ValidFacets(facetConfig = ProteomeFacetConfig.class)
    private String facets;

    @Positive(message = "{search.positive}")
    private Integer size;

    public static final String DEFAULT_FIELDS = "upid,organism,organism_id,protein_count";

    @Override
    public String getFields() {
        if (Strings.isNullOrEmpty(fields)) {
            fields = DEFAULT_FIELDS;
        } else if (!fields.contains(ProteomeField.Return.upid.name())) {
            String temp = "upid," + fields;
            this.fields = temp;
        }
        return fields;
    }
}
