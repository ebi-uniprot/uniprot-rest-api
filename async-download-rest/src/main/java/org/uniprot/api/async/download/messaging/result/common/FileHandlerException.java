package org.uniprot.api.async.download.messaging.result.common;

public class FileHandlerException extends RuntimeException {
    public FileHandlerException(Exception e) {
        super(e);
    }
}
