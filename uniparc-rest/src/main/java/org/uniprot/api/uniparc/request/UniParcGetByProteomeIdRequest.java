package org.uniprot.api.uniparc.request;

import org.uniprot.api.rest.request.SearchRequest;

import javax.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author sahmad
 * @created 13/08/2020
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UniParcGetByProteomeIdRequest extends UniParcGetByIdPageSearchRequest implements SearchRequest {
    private static final String PROTEOME_ID_STR = "upid";

    @Parameter(description = "UniProt Proteome UPID")
    @NotNull(message = "{search.required}")
    private String upId;

    @Override
    public String getQuery() {
        return PROTEOME_ID_STR + ":" + this.upId;
    }
}
