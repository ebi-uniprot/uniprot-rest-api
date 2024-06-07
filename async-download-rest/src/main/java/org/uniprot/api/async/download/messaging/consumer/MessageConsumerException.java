package org.uniprot.api.async.download.messaging.consumer;

import java.io.Serial;

public class MessageConsumerException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 102409437895437L;

    public MessageConsumerException(String message) {
        super(message);
    }

}
