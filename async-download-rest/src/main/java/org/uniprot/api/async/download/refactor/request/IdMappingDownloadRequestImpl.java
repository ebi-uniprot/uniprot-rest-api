package org.uniprot.api.async.download.refactor.request;

import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;
import static org.uniprot.api.rest.openapi.OpenAPIConstants.FIELDS_DESCRIPTION;

import javax.validation.constraints.NotNull;

import lombok.Data;

import org.springdoc.api.annotations.ParameterObject;
import org.uniprot.api.rest.request.UniProtKBRequestUtil;

import io.swagger.v3.oas.annotations.Parameter;

@Data
@ParameterObject
public class IdMappingDownloadRequestImpl implements IdMappingDownloadRequest {

    @Parameter(description = JOB_ID_IDMAPPING_DESCRIPTION)
    @NotNull(message = "{search.required}")
    private String jobId;

    @Parameter(description = FORMAT_UNIPROTKB_DESCRIPTION, example = FORMAT_UNIPROTKB_EXAMPLE)
    @NotNull(message = "{search.required}")
    private String format;

    @Parameter(description = FIELDS_DESCRIPTION)
    private String fields;

    @Parameter(hidden = true)
    private boolean force;

    @Override
    public void setFormat(String format) {
        this.format = UniProtKBRequestUtil.parseFormat(format);
    }
}
