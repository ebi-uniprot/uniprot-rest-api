package org.uniprot.api.uniparc.request;

import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;

import javax.validation.constraints.NotNull;

import org.springdoc.api.annotations.ParameterObject;
import org.uniprot.api.uniparc.common.service.request.UniParcGetByIdPageSearchRequest;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author sahmad
 * @created 13/08/2020
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ParameterObject
public class UniParcGetByDBRefIdRequest extends UniParcGetByIdPageSearchRequest {
    @Parameter(hidden = true)
    private static final String DB_ID_STR = "dbid";

    @Parameter(description = DBID_UNIPARC_DESCRIPTION, example = DBID_UNIPARC_EXAMPLE)
    @NotNull(message = "{search.required}")
    private String dbId;

    @Override
    public String getQuery() {
        return DB_ID_STR + ":" + this.dbId;
    }
}
