package org.uniprot.api.async.download.messaging.result.common;

public class FileHandelerException extends RuntimeException {
    public FileHandelerException(Exception e) {
        super(e);
    }
}
