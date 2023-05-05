package org.uniprot.api.rest.output.converter;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.AbstractGenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.validation.error.ErrorInfo;

public class ErrorMessageNTriplesConverter extends AbstractGenericHttpMessageConverter<ErrorInfo> {

    public ErrorMessageNTriplesConverter() {
        super(UniProtMediaType.N_TRIPLES_MEDIA_TYPE);
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return clazz.isAssignableFrom(ErrorInfo.class);
    }

    @Override
    protected void writeInternal(
            ErrorInfo errorInfo, Type type, HttpOutputMessage httpOutputMessage)
            throws IOException, HttpMessageNotWritableException {
        if (errorInfo.getMessages() != null) {
            OutputStream outputStream = httpOutputMessage.getBody();
            outputStream.write((errorInfo.getMessages() + "\n").getBytes());
        }
    }

    @Override
    protected ErrorInfo readInternal(
            Class<? extends ErrorInfo> aClass, HttpInputMessage httpInputMessage)
            throws HttpMessageNotReadableException {
        throw new UnsupportedOperationException("");
    }

    @Override
    public ErrorInfo read(Type type, Class<?> aClass, HttpInputMessage httpInputMessage)
            throws IOException, HttpMessageNotReadableException {
        throw new UnsupportedOperationException("");
    }
}
