package org.uniprot.api.async.download.model.idmapping;

public interface IdMappingDownloadRequest {

    String getJobId();

    String getFormat();

    String getFields();

    void setFormat(String format);

    boolean isForce();

    void setForce(boolean force);
}
