package uk.ac.ebi.uniprot.api.rest.output.converter;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.AbstractGenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType;
import uk.ac.ebi.uniprot.api.rest.validation.error.ResponseExceptionHandler;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * This class is reponsible to write error message body for http status BAD REQUESTS (400)
 *
 * @author lgonzales
 */
public class ErrorMessageConverter extends AbstractGenericHttpMessageConverter<ResponseExceptionHandler.ErrorInfo> {

    public ErrorMessageConverter(){
        super(UniProtMediaType.FF_MEDIA_TYPE,
                UniProtMediaType.FASTA_MEDIA_TYPE,
                UniProtMediaType.GFF_MEDIA_TYPE,
                UniProtMediaType.LIST_MEDIA_TYPE,
                UniProtMediaType.TSV_MEDIA_TYPE,
                UniProtMediaType.XLS_MEDIA_TYPE);
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return clazz.isAssignableFrom(ResponseExceptionHandler.ErrorInfo.class);
    }

    @Override
    protected void writeInternal(ResponseExceptionHandler.ErrorInfo errorInfo, Type type, HttpOutputMessage httpOutputMessage) throws IOException, HttpMessageNotWritableException {
        // we write nothing in the response body.
    }

    @Override
    protected ResponseExceptionHandler.ErrorInfo readInternal(Class<? extends ResponseExceptionHandler.ErrorInfo> aClass, HttpInputMessage httpInputMessage) throws IOException, HttpMessageNotReadableException {
        throw new UnsupportedOperationException("") ;
    }

    @Override
    public ResponseExceptionHandler.ErrorInfo read(Type type, Class<?> aClass, HttpInputMessage httpInputMessage) throws IOException, HttpMessageNotReadableException {
        throw new UnsupportedOperationException("") ;
    }
}
