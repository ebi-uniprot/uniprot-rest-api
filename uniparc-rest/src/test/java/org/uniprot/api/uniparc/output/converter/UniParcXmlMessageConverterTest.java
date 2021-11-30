package org.uniprot.api.uniparc.output.converter;

import org.springframework.http.MediaType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.output.converter.AbstractEntityHttpMessageConverter;
import org.uniprot.api.rest.output.converter.AbstractXmlValidationTest;
import org.uniprot.api.uniparc.output.MessageConverterConfig;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.store.indexer.uniparc.mockers.UniParcEntryMocker;

class UniParcXmlMessageConverterTest extends AbstractXmlValidationTest<UniParcEntry> {

    @Override
    public String getXSDUrlLocation() {
        return "https://www.uniprot.org/docs/uniparc.xsd";
    }

    @Override
    public MessageConverterContext<UniParcEntry> getMessageConverterConfig() {
        return new MessageConverterConfig()
                .uniparcMessageConverterContextFactory()
                .get(MessageConverterContextFactory.Resource.UNIPARC, MediaType.APPLICATION_XML);
    }

    @Override
    public AbstractEntityHttpMessageConverter<UniParcEntry> getXmlConverter() {
        return new UniParcXmlMessageConverter("2021_04");
    }

    @Override
    protected UniParcEntry getEntry() {
        return UniParcEntryMocker.createEntry(2, "UPI");
    }
}
