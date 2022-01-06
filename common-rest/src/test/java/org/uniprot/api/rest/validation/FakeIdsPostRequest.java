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
public class FakeIdsPostRequest implements IdsSearchRequest {
    private String accessions;
    private String fields;

    public String getCommaSeparatedIds() {
        return this.accessions;
    }

    @Override
    public String getDownload() {
        return null;
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

    @Override
    public Integer getSize() {
        return null;
    }

    @Override
    public void setSize(Integer size) {}
}
