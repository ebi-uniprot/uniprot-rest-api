package org.uniprot.api.uniparc.common.response.converter;

import java.io.IOException;
import java.io.OutputStream;

import javax.xml.bind.Marshaller;

import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.converter.AbstractXmlMessageConverter;
import org.uniprot.api.rest.output.converter.ConverterConstants;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.util.Utils;
import org.uniprot.core.xml.jaxb.uniparc.Entry;
import org.uniprot.core.xml.uniparc.UniParcEntryConverter;

/**
 * @author jluo
 * @date: 21 Jun 2019
 */
public class UniParcXmlMessageConverter extends AbstractXmlMessageConverter<UniParcEntry, Entry> {
    private String header;
    private final ThreadLocal<UniParcEntryConverter> XML_CONVERTER = new ThreadLocal<>();

    public UniParcXmlMessageConverter(String version) {
        this(version, null);
    }

    public UniParcXmlMessageConverter(String version, Gatekeeper downloadGatekeeper) {
        super(UniParcEntry.class, ConverterConstants.UNIPARC_XML_CONTEXT, downloadGatekeeper);
        header = ConverterConstants.UNIPARC_XML_SCHEMA;
        if (Utils.notNullNotEmpty(version)) {
            String versionAttrib = " version=\"" + version + "\"" + ">\n";
            header = header.replace(">\n", versionAttrib);
        }
        header = ConverterConstants.XML_DECLARATION + header;
    }

    @Override
    protected String getHeader() {
        return header;
    }

    @Override
    protected void before(MessageConverterContext<UniParcEntry> context, OutputStream outputStream)
            throws IOException {
        super.before(context, outputStream);
        XML_CONVERTER.set(new UniParcEntryConverter());
    }

    @Override
    protected Entry toXml(UniParcEntry entity) {
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
    protected void cleanUp() {
        super.cleanUp();
        XML_CONVERTER.remove();
    }
}
