package org.uniprot.api.proteome.output.converter;

import java.io.IOException;
import java.io.OutputStream;

import javax.xml.bind.Marshaller;

import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.converter.AbstractXmlMessageConverter;
import org.uniprot.core.proteome.ProteomeEntry;
import org.uniprot.core.xml.jaxb.proteome.Proteome;
import org.uniprot.core.xml.proteome.ProteomeConverter;

/**
 * @author jluo
 * @date: 29 Apr 2019
 */
public class ProteomeXmlMessageConverter
        extends AbstractXmlMessageConverter<ProteomeEntry, Proteome> {
    private final ThreadLocal<ProteomeConverter> XML_CONVERTER = new ThreadLocal<>();
    private static final String XML_CONTEXT = "org.uniprot.core.xml.jaxb.proteome";
    private static final String HEADER =
            "<proteomes xmlns=\"https://uniprot.org/uniprot\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"https://uniprot.org/uniprot https://www.uniprot.org/docs/proteome.xsd\">\n";

    private static final String FOOTER = "\n</proteomes>";

    public ProteomeXmlMessageConverter() {
        this(null);
    }

    public ProteomeXmlMessageConverter(Gatekeeper downloadGatekeeper) {
        super(ProteomeEntry.class, XML_CONTEXT, downloadGatekeeper);
    }

    @Override
    protected String getHeader() {
        return HEADER;
    }

    @Override
    protected void before(MessageConverterContext<ProteomeEntry> context, OutputStream outputStream)
            throws IOException {
        super.before(context, outputStream);
        XML_CONVERTER.set(new ProteomeConverter());
    }

    @Override
    protected Proteome toXml(ProteomeEntry entity) {
        return XML_CONVERTER.get().toXml(entity);
    }

    @Override
    protected Marshaller getMarshaller() {
        return createMarshaller();
    }

    @Override
    protected String getFooter() {
        return FOOTER;
    }

    @Override
    protected void cleanUp() {
        super.cleanUp();
        XML_CONVERTER.remove();
    }
}
