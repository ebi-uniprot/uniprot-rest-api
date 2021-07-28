package org.uniprot.api.uniparc.request;

import lombok.Data;

import org.uniprot.api.rest.request.IdsSearchRequest;
import org.uniprot.api.rest.validation.ValidDownloadByIdsRequest;
import org.uniprot.store.config.UniProtDataType;

/**
 * @author sahmad
 * @created 28/07/2021
 */
@Data
@ValidDownloadByIdsRequest(accessions = "upis", uniProtDataType = UniProtDataType.UNIPARC)
public class UniParcIdsDownloadRequest implements IdsSearchRequest {
    private String upis;
    private String fields;
    private String download;
    private Integer size;
    private String cursor;

    public String getCommaSeparatedIds() {
        return this.upis;
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
