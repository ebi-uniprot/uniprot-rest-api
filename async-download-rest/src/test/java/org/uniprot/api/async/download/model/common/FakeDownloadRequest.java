package org.uniprot.api.async.download.model.common;

import lombok.Data;

@Data
public class FakeDownloadRequest implements DownloadRequest {
    private String query = "dummy";
    private String fields;
    private String sort;
    private String download;
    private String format;
    private boolean isLargeSolrStreamRestricted;
    private boolean force;
}
