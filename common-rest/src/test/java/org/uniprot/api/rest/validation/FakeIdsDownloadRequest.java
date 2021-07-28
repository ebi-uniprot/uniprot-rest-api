package org.uniprot.api.rest.validation;

import lombok.Builder;
import lombok.Data;

import org.uniprot.api.rest.request.IdsSearchRequest;

/**
 * @author sahmad
 * @created 26/07/2021
 */
@Data
@Builder
public class FakeIdsDownloadRequest implements IdsSearchRequest {
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
