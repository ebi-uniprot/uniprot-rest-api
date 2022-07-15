package org.uniprot.api.idmapping.controller.request.uniprotkb;

import javax.validation.constraints.Pattern;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.uniprot.api.idmapping.controller.request.IdMappingPageRequest;
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
 * @created 18/02/2021
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UniProtKBIdMappingBasicRequest extends IdMappingPageRequest {

    @ModelFieldMeta(reader = QueryFieldMetaReaderImpl.class, path = "uniprotkb-search-fields.json")
    @Parameter(description = "Criteria to search the proteins. It can take any valid lucene query.")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(
            uniProtDataType = UniProtDataType.UNIPROTKB,
            messagePrefix = "search.uniprot")
    private String query;

    @ModelFieldMeta(reader = ReturnFieldMetaReaderImpl.class, path = "uniprotkb-return-fields.json")
    @Parameter(description = "Comma separated list of fields to be returned in response")
    @ValidReturnFields(uniProtDataType = UniProtDataType.UNIPROTKB)
    private String fields;

    @ModelFieldMeta(reader = SortFieldMetaReaderImpl.class, path = "uniprotkb-search-fields.json")
    @Parameter(description = "Name of the field to be sorted on")
    @ValidSolrSortFields(uniProtDataType = UniProtDataType.UNIPROTKB)
    private String sort;

    @Parameter(description = "Flag to include Isoform or not")
    @Pattern(
            regexp = "true|false",
            flags = {Pattern.Flag.CASE_INSENSITIVE},
            message = "{search.invalid.includeIsoform}")
    private String includeIsoform;

    public boolean isIncludeIsoform() {
        return Boolean.parseBoolean(includeIsoform);
    }
}
