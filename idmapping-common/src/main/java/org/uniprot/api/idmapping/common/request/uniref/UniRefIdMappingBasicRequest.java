package org.uniprot.api.idmapping.common.request.uniref;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.uniprot.api.idmapping.common.request.IdMappingPageRequest;
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
 * @author lgonzales
 * @since 25/02/2021
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UniRefIdMappingBasicRequest extends IdMappingPageRequest {
    @ModelFieldMeta(reader = QueryFieldMetaReaderImpl.class, path = "uniref-search-fields.json")
    @Parameter(description = "Criteria to search the proteins. It can take any valid lucene query.")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(uniProtDataType = UniProtDataType.UNIREF, messagePrefix = "search.uniref")
    private String query;

    @ModelFieldMeta(reader = ReturnFieldMetaReaderImpl.class, path = "uniref-return-fields.json")
    @Parameter(description = "Comma separated list of fields to be returned in response")
    @ValidReturnFields(uniProtDataType = UniProtDataType.UNIREF)
    private String fields;

    @ModelFieldMeta(reader = SortFieldMetaReaderImpl.class, path = "uniref-search-fields.json")
    @Parameter(description = "Name of the field to be sorted on")
    @ValidSolrSortFields(uniProtDataType = UniProtDataType.UNIREF)
    private String sort;

    @Parameter(hidden = true)
    private String format;
}
