package org.uniprot.api.aa.request;

import javax.validation.constraints.NotNull;

import lombok.Data;

import org.uniprot.api.rest.request.QueryFieldMetaReaderImpl;
import org.uniprot.api.rest.request.ReturnFieldMetaReaderImpl;
import org.uniprot.api.rest.request.SortFieldMetaReaderImpl;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.rest.validation.ValidSolrQueryFields;
import org.uniprot.api.rest.validation.ValidSolrQuerySyntax;
import org.uniprot.api.rest.validation.ValidSolrSortFields;
import org.uniprot.store.config.UniProtDataType;

import uk.ac.ebi.uniprot.openapi.extension.ModelFieldMeta;
import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author sahmad
 * @created 02/12/2020
 */
@Data
public class UniRuleBasicRequest {
    @ModelFieldMeta(reader = QueryFieldMetaReaderImpl.class, path = "unirule-search-fields.json")
    @Parameter(description = "Criteria to search UniRules. It can take any valid Lucene query.")
    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(
            uniProtDataType = UniProtDataType.UNIRULE,
            messagePrefix = "search.unirule")
    private String query;

    @ModelFieldMeta(reader = SortFieldMetaReaderImpl.class, path = "unirule-search-fields.json")
    @Parameter(description = "Name of the field to be sorted on")
    @ValidSolrSortFields(uniProtDataType = UniProtDataType.UNIRULE)
    private String sort;

    @ModelFieldMeta(reader = ReturnFieldMetaReaderImpl.class, path = "unirule-return-fields.json")
    @Parameter(description = "Comma separated list of fields to be returned in response")
    @ValidReturnFields(uniProtDataType = UniProtDataType.UNIRULE)
    private String fields;

    @Parameter(hidden = true)
    private String format;
}
