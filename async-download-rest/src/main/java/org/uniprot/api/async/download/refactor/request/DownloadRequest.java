package org.uniprot.api.async.download.refactor.request;

public interface DownloadRequest {
    void setId(String id);

    String getId();

    String getFormat();

    String getFields();

    boolean isForce();

    void setFormat(String format);

    void setForce(boolean force);
}
