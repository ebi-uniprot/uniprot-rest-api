package org.uniprot.api.rest.request;

public interface DownloadRequest extends StreamRequest {
    String getFormat();

    void setFormat(String format);

    boolean isLargeSolrStreamRestricted();

    void setLargeSolrStreamRestricted(boolean isRestricted);
}
