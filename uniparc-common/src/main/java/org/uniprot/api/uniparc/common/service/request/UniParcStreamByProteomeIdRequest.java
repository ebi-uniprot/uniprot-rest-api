package org.uniprot.api.uniparc.common.service.request;

import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.springdoc.api.annotations.ParameterObject;
import org.uniprot.api.rest.request.BasicRequest;
import org.uniprot.api.rest.request.StreamRequest;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.rest.validation.ValidSolrQueryFields;
import org.uniprot.api.rest.validation.ValidSolrQuerySyntax;
import org.uniprot.api.rest.validation.ValidSolrSortFields;
import org.uniprot.store.config.UniProtDataType;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@ParameterObject
public class UniParcStreamByProteomeIdRequest implements StreamRequest, BasicRequest {
    @Parameter(hidden = true)
    private static final String PROTEOME_ID_STR = "upid";

    @Parameter(description = DOWNLOAD_DESCRIPTION)
    @Pattern(regexp = "^true$|^false$", message = "{search.uniparc.invalid.download}")
    private String download;

    @Parameter(
            description = PROTEOME_UPID_UNIPARC_DESCRIPTION,
            example = PROTEOME_UPID_UNIPARC_EXAMPLE)
    @NotNull(message = "{search.required}")
    private String upId;

    @Parameter(description = QUERY_UNIPARC_DESCRIPTION, example = QUERY_UNIPARC_EXAMPLE)
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(
            uniProtDataType = UniProtDataType.UNIPARC,
            messagePrefix = "search.uniparc")
    private String query;

    @Parameter(description = SORT_UNIPARC_DESCRIPTION, example = SORT_UNIPARC_EXAMPLE)
    @ValidSolrSortFields(uniProtDataType = UniProtDataType.UNIPARC)
    private String sort;

    @Parameter(description = FIELDS_UNIPARC_DESCRIPTION, example = FIELDS_UNIPARC_EXAMPLE)
    @ValidReturnFields(uniProtDataType = UniProtDataType.UNIPARC)
    private String fields;

    @Parameter(hidden = true)
    private String format;

    @Override
    public String getQuery() {
        return PROTEOME_ID_STR + ":" + this.upId;
    }
}
