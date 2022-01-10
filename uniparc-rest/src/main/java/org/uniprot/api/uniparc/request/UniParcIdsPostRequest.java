package org.uniprot.api.uniparc.request;

import lombok.Data;

import org.uniprot.api.rest.request.IdsDownloadRequest;
import org.uniprot.api.rest.validation.ValidPostByIdsRequest;
import org.uniprot.store.config.UniProtDataType;

/**
 * @author sahmad
 * @created 28/07/2021
 */
@Data
@ValidPostByIdsRequest(accessions = "upis", uniProtDataType = UniProtDataType.UNIPARC)
public class UniParcIdsPostRequest extends IdsDownloadRequest {
    private String upis;
    private String fields;

    public String getCommaSeparatedIds() {
        return this.upis;
    }
}
