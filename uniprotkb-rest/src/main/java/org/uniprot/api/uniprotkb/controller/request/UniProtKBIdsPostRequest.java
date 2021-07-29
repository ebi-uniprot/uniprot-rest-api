package org.uniprot.api.uniprotkb.controller.request;

import lombok.Data;

import org.uniprot.api.rest.request.IdsSearchRequest;
import org.uniprot.api.rest.validation.ValidPostByIdsRequest;
import org.uniprot.store.config.UniProtDataType;

@Data
@ValidPostByIdsRequest(uniProtDataType = UniProtDataType.UNIPROTKB)
public class UniProtKBIdsPostRequest implements IdsSearchRequest {
    private String accessions;
    private String fields;
    private String download;
    private Integer size;

    public String getCommaSeparatedIds() {
        return this.accessions;
    }

    @Override
    public String getFacetFilter() {
        return null;
    }

    @Override
    public void setCursor(String cursor) {
        // do nothing
    }

    @Override
    public String getFacets() {
        return null;
    }

    @Override
    public String getCursor() {
        return null;
    }
}
