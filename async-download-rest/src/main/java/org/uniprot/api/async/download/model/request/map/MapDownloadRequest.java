package org.uniprot.api.async.download.model.request.map;

import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;
import static org.uniprot.api.rest.output.UniProtMediaType.*;

import org.uniprot.api.async.download.model.request.DownloadRequest;
import org.uniprot.api.async.download.model.request.SolrStreamDownloadRequest;

public interface MapDownloadRequest extends DownloadRequest, SolrStreamDownloadRequest {

    public String getFrom();

    public String getTo();
}
