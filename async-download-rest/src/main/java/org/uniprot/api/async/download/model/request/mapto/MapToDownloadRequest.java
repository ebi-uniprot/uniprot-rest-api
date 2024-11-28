package org.uniprot.api.async.download.model.request.mapto;

import org.uniprot.api.async.download.model.request.DownloadRequest;
import org.uniprot.api.async.download.model.request.SolrStreamDownloadRequest;

public interface MapToDownloadRequest extends DownloadRequest, SolrStreamDownloadRequest {

    String getFrom();

    String getTo();
}
