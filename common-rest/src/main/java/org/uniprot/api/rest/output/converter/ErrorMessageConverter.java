package org.uniprot.api.rest.output.converter;

import java.io.IOException;
import java.lang.reflect.Type;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.AbstractGenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.validation.error.ErrorInfo;

/**
 * This class is responsible to write error message body for http status BAD REQUESTS (400)
 *
 * @author lgonzales
 */
public class ErrorMessageConverter extends AbstractGenericHttpMessageConverter<ErrorInfo> {

    public ErrorMessageConverter() {
        super(
                UniProtMediaType.FF_MEDIA_TYPE,
                UniProtMediaType.FASTA_MEDIA_TYPE,
                UniProtMediaType.GFF_MEDIA_TYPE,
                UniProtMediaType.LIST_MEDIA_TYPE,
                UniProtMediaType.TSV_MEDIA_TYPE,
                UniProtMediaType.XLS_MEDIA_TYPE,
                UniProtMediaType.OBO_MEDIA_TYPE);
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return clazz.isAssignableFrom(ErrorInfo.class);
    }

    @Override
    protected void writeInternal(
            ErrorInfo errorInfo, Type type, HttpOutputMessage httpOutputMessage)
            throws IOException, HttpMessageNotWritableException {
        // we write nothing in the response body.
    }

    @Override
    protected ErrorInfo readInternal(
            Class<? extends ErrorInfo> aClass, HttpInputMessage httpInputMessage)
            throws IOException, HttpMessageNotReadableException {
        throw new UnsupportedOperationException("");
    }

    @Override
    public ErrorInfo read(Type type, Class<?> aClass, HttpInputMessage httpInputMessage)
            throws IOException, HttpMessageNotReadableException {
        throw new UnsupportedOperationException("");
    }
}
