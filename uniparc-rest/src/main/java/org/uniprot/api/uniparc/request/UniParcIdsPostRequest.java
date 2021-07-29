package org.uniprot.api.uniparc.request;

import lombok.Data;

import org.uniprot.api.rest.request.IdsSearchRequest;
import org.uniprot.api.rest.validation.ValidPostByIdsRequest;
import org.uniprot.store.config.UniProtDataType;

/**
 * @author sahmad
 * @created 28/07/2021
 */
@Data
@ValidPostByIdsRequest(accessions = "upis", uniProtDataType = UniProtDataType.UNIPARC)
public class UniParcIdsPostRequest implements IdsSearchRequest {
    private String upis;
    private String fields;
    private String download;
    private Integer size;

    public String getCommaSeparatedIds() {
        return this.upis;
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
