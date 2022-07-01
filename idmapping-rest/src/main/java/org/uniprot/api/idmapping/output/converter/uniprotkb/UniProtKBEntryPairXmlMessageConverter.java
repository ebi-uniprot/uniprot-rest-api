package org.uniprot.api.idmapping.output.converter.uniprotkb;

import java.io.IOException;
import java.io.OutputStream;

import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.idmapping.model.UniProtKBEntryPair;
import org.uniprot.api.idmapping.output.converter.EntryPairXmlMessageConverter;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.xml.jaxb.uniprot.Entry;
import org.uniprot.core.xml.uniprot.UniProtEntryConverter;

public class UniProtKBEntryPairXmlMessageConverter
        extends EntryPairXmlMessageConverter<UniProtKBEntryPair, UniProtKBEntry, Entry> {

    private final ThreadLocal<UniProtEntryConverter> XML_CONVERTER = new ThreadLocal<>();

    public UniProtKBEntryPairXmlMessageConverter(
            Class<UniProtKBEntryPair> messageConverterEntryClass,
            String context,
            String header,
            String footer,
            Gatekeeper downloadGatekeeper) {
        super(messageConverterEntryClass, context, header, footer, downloadGatekeeper);
    }

    @Override
    protected void before(
            MessageConverterContext<UniProtKBEntryPair> context, OutputStream outputStream)
            throws IOException {
        super.before(context, outputStream);
        XML_CONVERTER.set(new UniProtEntryConverter());
    }

    @Override
    protected Entry toXml(UniProtKBEntryPair entity) {
        return XML_CONVERTER.get().toXml(entity.getTo());
    }

    @Override
    protected void cleanUp() {
        super.cleanUp();
        XML_CONVERTER.remove();
    }
}
