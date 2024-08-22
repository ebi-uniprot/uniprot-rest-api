package org.uniprot.api.async.download.model.request;

import org.uniprot.api.rest.request.StreamRequest;

public interface SolrStreamDownloadRequest extends DownloadRequest, StreamRequest {
    void setLargeSolrStreamRestricted(boolean restricted);
}
