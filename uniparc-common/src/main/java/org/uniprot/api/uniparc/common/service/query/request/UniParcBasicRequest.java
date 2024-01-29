package org.uniprot.api.uniparc.common.service.query.request;

import javax.validation.constraints.NotNull;

import lombok.Data;

import org.uniprot.api.rest.request.QueryFieldMetaReaderImpl;
import org.uniprot.api.rest.request.ReturnFieldMetaReaderImpl;
import org.uniprot.api.rest.request.SortFieldMetaReaderImpl;
import org.uniprot.api.rest.request.UniProtKBRequestUtil;
import org.uniprot.api.rest.validation.*;
import org.uniprot.store.config.UniProtDataType;

import uk.ac.ebi.uniprot.openapi.extension.ModelFieldMeta;
import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author lgonzales
 * @since 18/06/2020
 */
@Data
public class UniParcBasicRequest {

    @ModelFieldMeta(reader = QueryFieldMetaReaderImpl.class, path = "uniparc-search-fields.json")
    @Parameter(description = "Criteria to search the uniparc. It can take any valid solr query.")
    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(
            uniProtDataType = UniProtDataType.UNIPARC,
            messagePrefix = "search.uniparc")
    protected String query;

    @ModelFieldMeta(reader = SortFieldMetaReaderImpl.class, path = "uniparc-search-fields.json")
    @Parameter(description = "Name of the field to be sorted on")
    @ValidSolrSortFields(uniProtDataType = UniProtDataType.UNIPARC)
    protected String sort;

    @ModelFieldMeta(reader = ReturnFieldMetaReaderImpl.class, path = "uniparc-return-fields.json")
    @Parameter(description = "Comma separated list of fields to be returned in response")
    @ValidReturnFields(uniProtDataType = UniProtDataType.UNIPARC)
    protected String fields;

    @Parameter(hidden = true)
    private String format;

    public void setFormat(String format) {
        this.format = UniProtKBRequestUtil.parseFormat(format);
    }
}
