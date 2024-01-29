package org.uniprot.api.uniparc.request;

import javax.validation.constraints.NotNull;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.uniprot.api.uniparc.common.service.query.request.UniParcGetByIdPageSearchRequest;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author sahmad
 * @created 13/08/2020
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UniParcGetByDBRefIdRequest extends UniParcGetByIdPageSearchRequest {
    @Parameter(hidden = true)
    private static final String DB_ID_STR = "dbid";

    @Parameter(
            description =
                    "UniParc cross reference id, eg. AAC02967 (EMBL) or XP_006524055 (RefSeq)")
    @NotNull(message = "{search.required}")
    private String dbId;

    @Override
    public String getQuery() {
        return DB_ID_STR + ":" + this.dbId;
    }
}
