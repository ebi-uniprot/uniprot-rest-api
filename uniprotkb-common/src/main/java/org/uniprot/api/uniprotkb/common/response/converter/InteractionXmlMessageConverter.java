package org.uniprot.api.uniprotkb.common.response.converter;

import java.io.IOException;
import java.io.OutputStream;

import org.springframework.http.MediaType;
import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.rest.output.converter.AbstractEntityHttpMessageConverter;
import org.uniprot.core.json.parser.uniprot.UniprotKBJsonConfig;
import org.uniprot.core.uniprotkb.interaction.InteractionEntry;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

/**
 * Created 13/05/2020
 *
 * @author Edd
 */
public class InteractionXmlMessageConverter
        extends AbstractEntityHttpMessageConverter<InteractionEntry> {
    private static final String ROOT_NAME = "InteractionEntry";
    private final ObjectWriter objectWriter;

    public InteractionXmlMessageConverter() {
        this(null);
    }

    public InteractionXmlMessageConverter(Gatekeeper downloadGatekeeper) {
        super(MediaType.APPLICATION_XML, InteractionEntry.class, downloadGatekeeper);
        JacksonXmlModule xmlModule = new JacksonXmlModule();
        xmlModule.setDefaultUseWrapper(false);

        XmlMapper xmlMapper = new XmlMapper(xmlModule);
        xmlMapper.registerModule(UniprotKBJsonConfig.getInstance().getPrettyWriterModule());
        objectWriter = xmlMapper.writer().withRootName(ROOT_NAME);
    }

    @Override
    protected void writeEntity(InteractionEntry entity, OutputStream outputStream)
            throws IOException {
        objectWriter.writeValue(outputStream, entity);
    }
}
