package org.uniprot.api.rest.request;

/**
 * @author sahmad
 * @created 30/07/2021
 */
public abstract class IdsDownloadRequest implements IdsSearchRequest {
    @Override
    public String getDownload() {
        return Boolean.TRUE.toString();
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
    public void setSize(Integer size) {
        // do nothing
    }
}
