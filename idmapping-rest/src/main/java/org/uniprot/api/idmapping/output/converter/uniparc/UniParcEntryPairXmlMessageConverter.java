package org.uniprot.api.idmapping.output.converter.uniparc;

import java.io.IOException;
import java.io.OutputStream;

import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.idmapping.model.UniParcEntryPair;
import org.uniprot.api.idmapping.output.converter.EntryPairXmlMessageConverter;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.xml.jaxb.uniparc.Entry;
import org.uniprot.core.xml.uniparc.UniParcEntryConverter;

public class UniParcEntryPairXmlMessageConverter
        extends EntryPairXmlMessageConverter<UniParcEntryPair, UniParcEntry, Entry> {

    private final ThreadLocal<UniParcEntryConverter> XML_CONVERTER = new ThreadLocal<>();

    public UniParcEntryPairXmlMessageConverter(
            Class<UniParcEntryPair> messageConverterEntryClass,
            String context,
            String header,
            String footer,
            Gatekeeper downloadGatekeeper) {
        super(messageConverterEntryClass, context, header, footer, downloadGatekeeper);
    }

    @Override
    protected void before(
            MessageConverterContext<UniParcEntryPair> context, OutputStream outputStream)
            throws IOException {
        super.before(context, outputStream);
        XML_CONVERTER.set(new UniParcEntryConverter());
    }

    @Override
    protected Entry toXml(UniParcEntryPair entity) {
        return XML_CONVERTER.get().toXml(entity.getTo());
    }

    @Override
    protected void cleanUp() {
        super.cleanUp();
        XML_CONVERTER.remove();
    }
}
