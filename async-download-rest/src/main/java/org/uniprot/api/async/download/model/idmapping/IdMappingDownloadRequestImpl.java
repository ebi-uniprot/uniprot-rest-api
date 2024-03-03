package org.uniprot.api.async.download.model.idmapping;

import javax.validation.constraints.NotNull;

import lombok.Data;

import org.uniprot.api.rest.request.UniProtKBRequestUtil;

@Data
public class IdMappingDownloadRequestImpl implements IdMappingDownloadRequest {

    @NotNull(message = "{search.required}")
    private String jobId;

    @NotNull(message = "{search.required}")
    private String format;

    private String fields;

    private boolean force;

    @Override
    public void setFormat(String format) {
        this.format = UniProtKBRequestUtil.parseFormat(format);
    }
}
