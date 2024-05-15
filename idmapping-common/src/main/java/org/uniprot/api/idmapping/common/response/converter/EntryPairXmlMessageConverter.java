package org.uniprot.api.idmapping.common.response.converter;

import javax.xml.bind.Marshaller;

import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.common.repository.search.EntryPair;
import org.uniprot.api.rest.output.converter.AbstractXmlMessageConverter;

/**
 * @author sahmad
 * @created 02/03/2021
 */
public abstract class EntryPairXmlMessageConverter<T extends EntryPair<U>, U, V>
        extends AbstractXmlMessageConverter<T, V> {
    private final String header;
    private final String footer;

    public EntryPairXmlMessageConverter(
            Class<T> messageConverterEntryClass,
            String context,
            String header,
            String footer,
            Gatekeeper downloadGatekeeper) {
        super(messageConverterEntryClass, context, downloadGatekeeper);
        this.header = header;
        this.footer = footer;
    }

    @Override
    protected Marshaller getMarshaller() {
        return createMarshaller();
    }

    @Override
    protected String getFooter() {
        return this.footer;
    }

    @Override
    protected String getHeader() {
        return this.header;
    }
}
