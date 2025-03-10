package org.uniprot.api.uniparc.common.service.request;

import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;

import javax.validation.constraints.Pattern;

import org.springdoc.api.annotations.ParameterObject;
import org.uniprot.api.rest.request.StreamRequest;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.store.config.UniProtDataType;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@ParameterObject
public abstract class UniParcGetByIdStreamRequest extends UniParcGetByIdRequest
        implements StreamRequest {

    @Parameter(description = DOWNLOAD_DESCRIPTION)
    @Pattern(regexp = "^true$|^false$", message = "{search.uniparc.invalid.download}")
    private String download;

    @Parameter(description = FIELDS_UNIPARC_DESCRIPTION, example = FIELDS_UNIPARC_EXAMPLE)
    @ValidReturnFields(uniProtDataType = UniProtDataType.UNIPARC)
    private String fields;

    @Override
    public String getSort() {
        return null;
    }
}
