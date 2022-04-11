package org.uniprot.api.uniparc.request;

import lombok.Data;
import lombok.EqualsAndHashCode;
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

    @Parameter(hidden = true)
    private String dbId;

    @Override
    public String getQuery() {
        return DB_ID_STR + ":" + this.dbId;
    }
}
