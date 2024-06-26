package org.uniprot.api.async.download.model.request.idmapping;

import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;
import static org.uniprot.api.rest.openapi.OpenAPIConstants.FIELDS_DESCRIPTION;

import javax.validation.constraints.NotNull;

import org.springdoc.api.annotations.ParameterObject;
import org.uniprot.api.async.download.model.request.DownloadRequest;
import org.uniprot.api.rest.request.UniProtKBRequestUtil;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

@Data
@ParameterObject
public class IdMappingDownloadRequest implements DownloadRequest {

    @Parameter(description = JOB_ID_IDMAPPING_DESCRIPTION)
    @NotNull(message = "'jobId' is a required parameter")
    @JsonProperty("jobId")
    private String idMappingJobId;

    @Parameter(description = FORMAT_UNIPROTKB_DESCRIPTION, example = FORMAT_UNIPROTKB_EXAMPLE)
    @NotNull(message = "{search.required}")
    private String format;

    @Parameter(description = FIELDS_DESCRIPTION)
    private String fields;

    @Parameter(hidden = true)
    private boolean force;

    @Parameter(hidden = true)
    private String downloadJobId;

    @Override
    public void setFormat(String format) {
        this.format = UniProtKBRequestUtil.parseFormat(format);
    }

    public void setJobId(String jobId) {
        this.idMappingJobId = jobId;
    }
}
