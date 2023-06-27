package org.uniprot.api.uniprotkb.controller.request;

import lombok.Data;

import org.uniprot.api.rest.request.IdsDownloadRequest;
import org.uniprot.api.rest.validation.ValidPostByIdsRequest;
import org.uniprot.store.config.UniProtDataType;

@Data
@ValidPostByIdsRequest(uniProtDataType = UniProtDataType.UNIPROTKB)
public class UniProtKBIdsPostRequest extends IdsDownloadRequest {
    private String accessions;
    private String fields;
    private String format;

    public String getCommaSeparatedIds() {
        return this.accessions;
    }
}
