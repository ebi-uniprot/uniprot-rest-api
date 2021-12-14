package org.uniprot.api.uniref.output.converter;

import static org.junit.jupiter.api.Assertions.*;

import org.springframework.http.MediaType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.output.converter.AbstractEntityHttpMessageConverter;
import org.uniprot.api.rest.output.converter.AbstractXmlValidationTest;
import org.uniprot.api.uniref.output.UniRefMessageConverterConfig;
import org.uniprot.core.uniref.UniRefEntry;
import org.uniprot.core.uniref.UniRefType;
import org.uniprot.store.indexer.uniref.mockers.UniRefEntryMocker;

class UniRefXmlMessageConverterTest extends AbstractXmlValidationTest<UniRefEntry> {

    @Override
    public String getXSDUrlLocation() {
        return "https://www.uniprot.org/docs/uniref.xsd";
    }

    @Override
    public MessageConverterContext<UniRefEntry> getMessageConverterConfig() {
        return new UniRefMessageConverterConfig()
                .uniRefMessageConverterContextFactory()
                .get(MessageConverterContextFactory.Resource.UNIREF, MediaType.APPLICATION_XML);
    }

    @Override
    public AbstractEntityHttpMessageConverter<UniRefEntry> getXmlConverter() {
        return new UniRefXmlMessageConverter("2021_04", "2021-09-29");
    }

    @Override
    protected UniRefEntry getEntry() {
        return UniRefEntryMocker.createEntry(1, UniRefType.UniRef100);
    }
}
