package org.uniprot.api.support.data.taxonomy.request;

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
 * @created 23/01/2021
 */
@Data
public class TaxonomyBasicRequest {
    @ModelFieldMeta(reader = QueryFieldMetaReaderImpl.class, path = "taxonomy-search-fields.json")
    @Parameter(description = "Criteria to search taxonomies. It can take any valid solr query.")
    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(
            uniProtDataType = UniProtDataType.TAXONOMY,
            messagePrefix = "search.taxonomy")
    private String query;

    @ModelFieldMeta(reader = SortFieldMetaReaderImpl.class, path = "taxonomy-search-fields.json")
    @Parameter(description = "Name of the field to be sorted on")
    @ValidSolrSortFields(uniProtDataType = UniProtDataType.TAXONOMY)
    private String sort;

    @ModelFieldMeta(reader = ReturnFieldMetaReaderImpl.class, path = "taxonomy-return-fields.json")
    @Parameter(description = "Comma separated list of fields to be returned in response")
    @ValidReturnFields(uniProtDataType = UniProtDataType.TAXONOMY)
    private String fields;
}
