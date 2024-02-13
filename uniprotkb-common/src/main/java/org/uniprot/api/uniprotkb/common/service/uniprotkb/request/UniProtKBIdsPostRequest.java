package org.uniprot.api.uniprotkb.common.service.uniprotkb.request;

import lombok.Data;

import org.springdoc.api.annotations.ParameterObject;
import org.uniprot.api.rest.request.IdsDownloadRequest;
import org.uniprot.api.rest.validation.ValidPostByIdsRequest;
import org.uniprot.store.config.UniProtDataType;

@Data
@ValidPostByIdsRequest(uniProtDataType = UniProtDataType.UNIPROTKB)
@ParameterObject
public class UniProtKBIdsPostRequest extends IdsDownloadRequest {
    private String accessions;
    private String fields;
    private String format;

    public String getCommaSeparatedIds() {
        return this.accessions;
    }
}
