package org.uniprot.api.uniref.output.converter;

import java.io.IOException;
import java.io.OutputStream;

import javax.xml.bind.Marshaller;

import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.converter.AbstractXmlMessageConverter;
import org.uniprot.api.rest.output.converter.ConverterConstants;
import org.uniprot.core.uniref.UniRefEntry;
import org.uniprot.core.xml.jaxb.uniref.Entry;
import org.uniprot.core.xml.uniref.UniRefEntryConverter;

/**
 * @author jluo
 * @date: 22 Aug 2019
 */
public class UniRefXmlMessageConverter extends AbstractXmlMessageConverter<UniRefEntry, Entry> {
    private final ThreadLocal<UniRefEntryConverter> XML_CONVERTER = new ThreadLocal<>();
    private String header;

    public UniRefXmlMessageConverter(String version, String releaseDate) {
        this(version, releaseDate, null);
    }

    public UniRefXmlMessageConverter(
            String version, String releaseDate, Gatekeeper downloadGatekeeper) {
        super(UniRefEntry.class, ConverterConstants.UNIREF_XML_CONTEXT, downloadGatekeeper);
        header = ConverterConstants.UNIREF_XML_SCHEMA;
        if ((version != null) && (!version.isEmpty())) {
            header += " version=\"" + version + "\"";
        }

        if ((releaseDate != null) && (!releaseDate.isEmpty())) {
            header += " releaseDate=\"" + releaseDate + "\"";
        }
        header += ">\n";

        header = ConverterConstants.XML_DECLARATION + header;
    }

    @Override
    protected String getHeader() {
        return header;
    }

    @Override
    protected void before(MessageConverterContext<UniRefEntry> context, OutputStream outputStream)
            throws IOException {
        super.before(context, outputStream);
        XML_CONVERTER.set(new UniRefEntryConverter());
    }

    @Override
    protected Entry toXml(UniRefEntry entity) {
        return XML_CONVERTER.get().toXml(entity);
    }

    @Override
    protected Marshaller getMarshaller() {
        return createMarshaller();
    }

    @Override
    protected String getFooter() { // do not add copyright tag see TRM-27009
        return ConverterConstants.UNIREF_XML_CLOSE_TAG;
    }

    @Override
    protected void cleanUp() {
        super.cleanUp();
        XML_CONVERTER.remove();
    }
}
