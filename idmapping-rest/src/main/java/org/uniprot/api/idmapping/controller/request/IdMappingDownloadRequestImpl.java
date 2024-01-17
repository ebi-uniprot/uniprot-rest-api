package org.uniprot.api.idmapping.controller.request;

import javax.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

import org.springdoc.api.annotations.ParameterObject;
import org.uniprot.api.rest.request.UniProtKBRequestUtil;

@Data
@ParameterObject
public class IdMappingDownloadRequestImpl implements IdMappingDownloadRequest {

    @Parameter(description = "Unique identifier for idmapping job")
    @NotNull(message = "{search.required}")
    private String jobId;

    @Parameter(description = "Download response format, for example: json")
    @NotNull(message = "{search.required}")
    private String format;

    @Parameter(description = "Comma separated list of fields to be returned in response")
    private String fields;

    @Override
    public void setFormat(String format) {
        this.format = UniProtKBRequestUtil.parseFormat(format);
    }
}
