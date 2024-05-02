package org.uniprot.api.uniparc.common.service.request;

import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;

import javax.validation.constraints.NotNull;

import org.uniprot.api.rest.request.BasicRequest;
import org.uniprot.api.rest.request.UniProtKBRequestUtil;
import org.uniprot.api.rest.validation.*;
import org.uniprot.store.config.UniProtDataType;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

/**
 * @author lgonzales
 * @since 18/06/2020
 */
@Data
public class UniParcBasicRequest implements BasicRequest {

    @Parameter(description = QUERY_UNIPARC_DESCRIPTION, example = QUERY_UNIPARC_EXAMPLE)
    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(
            uniProtDataType = UniProtDataType.UNIPARC,
            messagePrefix = "search.uniparc")
    protected String query;

    @Parameter(description = SORT_UNIPARC_DESCRIPTION, example = SORT_UNIPARC_EXAMPLE)
    @ValidSolrSortFields(uniProtDataType = UniProtDataType.UNIPARC)
    protected String sort;

    @Parameter(description = FIELDS_UNIPARC_DESCRIPTION, example = FIELDS_UNIPARC_EXAMPLE)
    @ValidReturnFields(uniProtDataType = UniProtDataType.UNIPARC)
    protected String fields;

    @Parameter(hidden = true)
    private String format;

    public void setFormat(String format) {
        this.format = UniProtKBRequestUtil.parseFormat(format);
    }
}
