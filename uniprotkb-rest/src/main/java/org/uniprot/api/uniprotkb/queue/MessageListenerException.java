package org.uniprot.api.uniprotkb.queue;

public class MessageListenerException extends RuntimeException {
    private static final long serialVersionUID = 102409437895437L;
    public MessageListenerException(String message){
        super(message);
    }

    public MessageListenerException(String message, Throwable throwable){
        super(message, throwable);
    }

    public MessageListenerException(Throwable throwable){
        super(throwable);
    }
}
