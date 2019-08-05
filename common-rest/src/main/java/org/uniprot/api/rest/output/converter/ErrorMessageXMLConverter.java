package org.uniprot.api.rest.output.converter;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractGenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.uniprot.api.rest.validation.error.ResponseExceptionHandler;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;

/**
 *
 * @author lgonzales
 */
public class ErrorMessageXMLConverter extends AbstractGenericHttpMessageConverter<ResponseExceptionHandler.ErrorInfo> {

    private final Marshaller marshaller;

    public ErrorMessageXMLConverter(){
        super(MediaType.APPLICATION_XML);
        marshaller = createMarshaller();
    }

    @Override
    public ResponseExceptionHandler.ErrorInfo read(Type type, Class<?> aClass, HttpInputMessage httpInputMessage) throws HttpMessageNotReadableException {
        return null;
    }

    @Override
    protected ResponseExceptionHandler.ErrorInfo readInternal(Class<? extends ResponseExceptionHandler.ErrorInfo> aClass, HttpInputMessage httpInputMessage) throws HttpMessageNotReadableException {
        return null;
    }

    @Override
    protected void writeInternal(ResponseExceptionHandler.ErrorInfo entity, Type type, HttpOutputMessage httpOutputMessage) throws IOException, HttpMessageNotWritableException {
        try {
            OutputStream outputStream = httpOutputMessage.getBody();
            marshaller.marshal(entity, outputStream);
        } catch (JAXBException e) {
            throw new RuntimeException("JAXB entity write failed for ErrorMessageXMLConverter", e);
        }
    }


    private Marshaller createMarshaller() {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(ResponseExceptionHandler.ErrorInfo.class);
            Marshaller contextMarshaller = jaxbContext.createMarshaller();
            contextMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            contextMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            return contextMarshaller;
        } catch (Exception e) {
            throw new RuntimeException("JAXB initialisation failed for ErrorMessageXMLConverter", e);
        }
    }
}
