package org.uniprot.api.async.download.model.request;

public interface DownloadRequest {
    void setDownloadJobId(String jobId);

    String getDownloadJobId();

    String getFormat();

    String getFields();

    boolean isForce();

    void setFormat(String format);

    void setForce(boolean force);
}
