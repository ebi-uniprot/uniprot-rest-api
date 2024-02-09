package org.uniprot.api.idmapping.common.request;

public interface IdMappingDownloadRequest {

    String getJobId();

    String getFormat();

    String getFields();

    void setFormat(String format);

    boolean isForce();

    void setForce(boolean force);
}
