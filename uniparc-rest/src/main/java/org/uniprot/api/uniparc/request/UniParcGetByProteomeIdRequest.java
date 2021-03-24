package org.uniprot.api.uniparc.request;

import javax.validation.constraints.NotNull;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.uniprot.api.rest.request.ReturnFieldMetaReaderImpl;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.store.config.UniProtDataType;

import io.swagger.v3.oas.annotations.Parameter;
import uk.ac.ebi.uniprot.openapi.extension.ModelFieldMeta;

/**
 * @author sahmad
 * @created 13/08/2020
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UniParcGetByProteomeIdRequest extends UniParcGetByIdPageSearchRequest
        implements SearchRequest {
    @Parameter(hidden = true)
    private static final String PROTEOME_ID_STR = "upid";

    @Parameter(description = "UniProt Proteome UPID")
    @NotNull(message = "{search.required}")
    private String upId;

    @Override
    public String getQuery() {
        return PROTEOME_ID_STR + ":" + this.upId;
    }
}
