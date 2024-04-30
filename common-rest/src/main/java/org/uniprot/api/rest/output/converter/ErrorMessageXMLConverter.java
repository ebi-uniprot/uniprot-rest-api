package org.uniprot.api.rest.output.converter;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractGenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.lang.Nullable;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.validation.error.ErrorInfo;

/** @author lgonzales */
public class ErrorMessageXMLConverter extends AbstractGenericHttpMessageConverter<ErrorInfo> {

    private final Marshaller marshaller;

    public ErrorMessageXMLConverter() {
        super(MediaType.APPLICATION_XML, UniProtMediaType.RDF_MEDIA_TYPE);
        marshaller = createMarshaller();
    }

    @Override
    public boolean canWrite(@Nullable Type type, Class<?> clazz, @Nullable MediaType mediaType) {
        return clazz == ErrorInfo.class && this.canWrite(mediaType);
    }

    @Override
    public ErrorInfo read(Type type, Class<?> aClass, HttpInputMessage httpInputMessage)
            throws HttpMessageNotReadableException {
        return null;
    }

    @Override
    protected ErrorInfo readInternal(
            Class<? extends ErrorInfo> aClass, HttpInputMessage httpInputMessage)
            throws HttpMessageNotReadableException {
        return null;
    }

    @Override
    protected void writeInternal(ErrorInfo entity, Type type, HttpOutputMessage httpOutputMessage)
            throws IOException, HttpMessageNotWritableException {
        try {
            OutputStream outputStream = httpOutputMessage.getBody();
            marshaller.marshal(entity, outputStream);
        } catch (JAXBException e) {
            throw new RuntimeException("JAXB entity write failed for ErrorMessageXMLConverter", e);
        }
    }

    private Marshaller createMarshaller() {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(ErrorInfo.class);
            Marshaller contextMarshaller = jaxbContext.createMarshaller();
            contextMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            contextMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            return contextMarshaller;
        } catch (Exception e) {
            throw new RuntimeException(
                    "JAXB initialisation failed for ErrorMessageXMLConverter", e);
        }
    }
}
