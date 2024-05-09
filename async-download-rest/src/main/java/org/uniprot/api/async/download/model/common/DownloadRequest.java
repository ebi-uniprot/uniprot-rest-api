package org.uniprot.api.async.download.model.common;

import org.uniprot.api.rest.request.StreamRequest;

public interface DownloadRequest extends StreamRequest {
    String getFormat();

    void setFormat(String format);

    boolean isLargeSolrStreamRestricted();

    void setLargeSolrStreamRestricted(boolean isRestricted);

    boolean isForce();

    void setForce(boolean force);
}
