package org.uniprot.api.proteome.request;

import io.swagger.v3.oas.annotations.Parameter;
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

import javax.validation.constraints.NotNull;

/**
 * @author lgonzales
 * @since 23/11/2020
 */
@Data
public class ProteomeBasicRequest {

    @ModelFieldMeta(reader = QueryFieldMetaReaderImpl.class, path = "proteome-search-fields.json")
    @Parameter(description = "Criteria to search the proteome. It can take any valid solr query.")
    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(
            uniProtDataType = UniProtDataType.PROTEOME,
            messagePrefix = "search.proteome")
    private String query;

    @ModelFieldMeta(reader = SortFieldMetaReaderImpl.class, path = "proteome-search-fields.json")
    @Parameter(description = "Name of the field to be sorted on")
    @ValidSolrSortFields(uniProtDataType = UniProtDataType.PROTEOME)
    private String sort;

    @ModelFieldMeta(reader = ReturnFieldMetaReaderImpl.class, path = "proteome-return-fields.json")
    @Parameter(description = "Comma separated list of fields to be returned in response")
    @ValidReturnFields(uniProtDataType = UniProtDataType.PROTEOME)
    private String fields;
}
