package org.uniprot.api.uniprotkb.controller.request;

import lombok.Data;

import org.uniprot.api.rest.request.IdsSearchRequest;
import org.uniprot.api.rest.validation.ValidDownloadByIdsRequest;
import org.uniprot.store.config.UniProtDataType;

@Data
@ValidDownloadByIdsRequest(uniProtDataType = UniProtDataType.UNIPROTKB)
public class UniProtKBIdsDownloadRequest implements IdsSearchRequest {
    private String accessions;
    private String fields;
    private String download;
    private Integer size;
    private String cursor;

    public String getCommaSeparatedIds() {
        return this.accessions;
    }

    @Override
    public String getFacetFilter() {
        return null;
    }

    @Override
    public String getFacets() {
        return null;
    }
}
