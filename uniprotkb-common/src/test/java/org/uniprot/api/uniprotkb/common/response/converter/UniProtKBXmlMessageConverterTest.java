package org.uniprot.api.uniprotkb.common.response.converter;

import static org.uniprot.store.indexer.uniprot.mockers.UniProtEntryMocker.Type.SP_COMPLEX;

import org.junit.jupiter.api.Disabled;
import org.springframework.http.MediaType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.output.converter.AbstractEntityHttpMessageConverter;
import org.uniprot.api.rest.output.converter.AbstractXmlValidationTest;
import org.uniprot.api.uniprotkb.common.response.UniProtKBMessageConverterConfig;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.indexer.uniprot.mockers.UniProtEntryMocker;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Disabled
class UniProtKBXmlMessageConverterTest extends AbstractXmlValidationTest<UniProtKBEntry> {

    @Override
    public UniProtKBEntry getEntry() {
        // add some more features to this entry that are causing problems!
        return UniProtEntryMocker.create(SP_COMPLEX);
    }

    @Override
    public String getXSDUrlLocation() {
        return "https://www.uniprot.org/docs/uniprot.xsd";
    }

    @Override
    public AbstractEntityHttpMessageConverter<UniProtKBEntry> getXmlConverter() {
        return new UniProtKBXmlMessageConverter();
    }

    @Override
    public MessageConverterContext<UniProtKBEntry> getMessageConverterConfig() {
        return new UniProtKBMessageConverterConfig()
                .messageConverterContextFactory()
                .get(MessageConverterContextFactory.Resource.UNIPROTKB, MediaType.APPLICATION_XML);
    }
}
