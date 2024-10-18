package org.uniprot.api.uniparc.common.response.converter;

import java.io.IOException;
import java.io.OutputStream;

import javax.xml.bind.Marshaller;

import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.converter.AbstractXmlMessageConverter;
import org.uniprot.api.rest.output.converter.ConverterConstants;
import org.uniprot.core.uniparc.UniParcEntryLight;
import org.uniprot.core.util.Utils;
import org.uniprot.core.xml.jaxb.uniparc.Entry;
import org.uniprot.core.xml.uniparc.UniParcEntryLightConverter;

public class UniParcLightXMLMessageConverter
        extends AbstractXmlMessageConverter<UniParcEntryLight, Entry> {
    private String header;
    private final ThreadLocal<UniParcEntryLightConverter> XML_CONVERTER = new ThreadLocal<>();

    public UniParcLightXMLMessageConverter(String version) {
        this(version, null);
    }

    public UniParcLightXMLMessageConverter(String version, Gatekeeper downloadGatekeeper) {
        super(UniParcEntryLight.class, ConverterConstants.UNIPARC_XML_CONTEXT, downloadGatekeeper);
        header = ConverterConstants.UNIPARC_XML_SCHEMA;
        if (Utils.notNullNotEmpty(version)) {
            String versionAttrib = " version=\"" + version + "\"" + ">\n";
            header = header.replace(">\n", versionAttrib);
        }
        header = ConverterConstants.XML_DECLARATION + header;
    }

    @Override
    protected Entry toXml(UniParcEntryLight entity) {
        return XML_CONVERTER.get().toXml(entity);
    }

    @Override
    protected Marshaller getMarshaller() {
        return createMarshaller();
    }

    @Override
    protected String getFooter() {
        return ConverterConstants.COPYRIGHT_TAG + ConverterConstants.UNIPARC_XML_CLOSE_TAG;
    }

    @Override
    protected String getHeader() {
        return this.header;
    }

    @Override
    protected void before(
            MessageConverterContext<UniParcEntryLight> context, OutputStream outputStream)
            throws IOException {
        super.before(context, outputStream);
        XML_CONVERTER.set(new UniParcEntryLightConverter());
    }

    @Override
    protected void cleanUp() {
        super.cleanUp();
        XML_CONVERTER.remove();
    }
}
