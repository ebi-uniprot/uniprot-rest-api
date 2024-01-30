package org.uniprot.api.idmapping.controller.request;

import javax.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

import org.springdoc.api.annotations.ParameterObject;
import org.uniprot.api.rest.request.UniProtKBRequestUtil;

import static org.uniprot.api.rest.openapi.OpenApiConstants.*;

@Data
@ParameterObject
public class IdMappingDownloadRequestImpl implements IdMappingDownloadRequest {

    @Parameter(description = ID_MAPPING_JOB_ID_DESCRIPTION)
    @NotNull(message = "{search.required}")
    private String jobId;

    @Parameter(description = FORMAT_UNIPROTKB_DESCRIPTION, example = "json")
    @NotNull(message = "{search.required}")
    private String format;

    @Parameter(description = FIELDS_DESCRIPTION)
    private String fields;

    @Override
    public void setFormat(String format) {
        this.format = UniProtKBRequestUtil.parseFormat(format);
    }
}
