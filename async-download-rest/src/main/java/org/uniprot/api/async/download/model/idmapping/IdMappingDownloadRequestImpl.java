package org.uniprot.api.async.download.model.idmapping;

import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;

import javax.validation.constraints.NotNull;

import org.springdoc.api.annotations.ParameterObject;
import org.uniprot.api.rest.request.UniProtKBRequestUtil;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

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