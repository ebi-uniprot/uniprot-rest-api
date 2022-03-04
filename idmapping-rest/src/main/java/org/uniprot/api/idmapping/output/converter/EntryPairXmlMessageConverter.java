package org.uniprot.api.idmapping.output.converter;

import javax.xml.bind.Marshaller;

import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.idmapping.model.EntryPair;
import org.uniprot.api.rest.output.converter.AbstractXmlMessageConverter;
import org.uniprot.core.xml.Converter;

/**
 * @author sahmad
 * @created 02/03/2021
 */
public class EntryPairXmlMessageConverter<T extends EntryPair<U>, U, V>
        extends AbstractXmlMessageConverter<T, V> {
    private final Converter<V, U> converter;
    private final String header;
    private final String footer;

    public EntryPairXmlMessageConverter(
            Class<T> messageConverterEntryClass,
            String context,
            Converter<V, U> converter,
            String header,
            String footer) {
        super(messageConverterEntryClass, context);
        this.converter = converter;
        this.header = header;
        this.footer = footer;
    }

    public EntryPairXmlMessageConverter(
            Class<T> messageConverterEntryClass,
            String context,
            Converter<V, U> converter,
            String header,
            String footer,
            Gatekeeper downloadGatekeeper) {
        super(messageConverterEntryClass, context, downloadGatekeeper);
        this.converter = converter;
        this.header = header;
        this.footer = footer;
    }

    @Override
    protected V toXml(T entryPair) {
        return this.converter.toXml(entryPair.getTo());
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
