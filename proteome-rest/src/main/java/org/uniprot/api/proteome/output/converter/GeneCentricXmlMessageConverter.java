package org.uniprot.api.proteome.output.converter;

import java.io.IOException;
import java.io.OutputStream;

import org.springframework.http.MediaType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.converter.AbstractEntityHttpMessageConverter;
import org.uniprot.core.genecentric.GeneCentricEntry;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

/**
 * @author jluo
 * @date: 21 Jun 2019
 */
public class GeneCentricXmlMessageConverter
        extends AbstractEntityHttpMessageConverter<GeneCentricEntry> {

    private final XmlMapper mapper;

    public GeneCentricXmlMessageConverter() {
        super(MediaType.APPLICATION_XML, GeneCentricEntry.class);
        JacksonXmlModule xmlModule = new JacksonXmlModule();
        xmlModule.setDefaultUseWrapper(false);

        mapper = new XmlMapper(xmlModule);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }

    @Override
    protected void writeEntity(GeneCentricEntry entity, OutputStream outputStream)
            throws IOException {
        byte[] output = mapper.writer().withRootName("GeneCentric").writeValueAsBytes(entity);
        outputStream.write(output);
    }

    @Override
    protected void before(
            MessageConverterContext<GeneCentricEntry> context, OutputStream outputStream)
            throws IOException {
        outputStream.write("<GeneCentrics>".getBytes());
    }

    @Override
    protected void after(
            MessageConverterContext<GeneCentricEntry> context, OutputStream outputStream)
            throws IOException {
        outputStream.write("</GeneCentrics>".getBytes());
    }
}
