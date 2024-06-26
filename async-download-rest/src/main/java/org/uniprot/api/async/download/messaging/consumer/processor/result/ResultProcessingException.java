package org.uniprot.api.async.download.messaging.consumer.processor.result;

public class ResultProcessingException extends RuntimeException {
    public ResultProcessingException(String message) {
        super(message);
    }
}
