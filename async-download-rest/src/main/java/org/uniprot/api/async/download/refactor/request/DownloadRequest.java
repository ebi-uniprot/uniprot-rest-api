package org.uniprot.api.async.download.refactor.request;

public interface DownloadRequest {

    String getFormat();

    String getFields();

    boolean isForce();

    void setFormat(String format);

    void setForce(boolean force);
}
