package org.uniprot.api.uniparc.common.response.converter;

import org.junit.jupiter.api.Disabled;
import org.springframework.http.MediaType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.output.converter.AbstractEntityHttpMessageConverter;
import org.uniprot.api.rest.output.converter.AbstractXmlValidationTest;
import org.uniprot.api.uniparc.common.response.UniParcMessageConverterConfig;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.store.indexer.uniparc.mockers.UniParcEntryMocker;

@Disabled
class UniParcXmlMessageConverterTest extends AbstractXmlValidationTest<UniParcEntry> {

    @Override
    public String getXSDUrlLocation() {
        return "https://www.uniprot.org/docs/uniparc.xsd";
    }

    @Override
    public MessageConverterContext<UniParcEntry> getMessageConverterConfig() {
        return new UniParcMessageConverterConfig()
                .uniparcMessageConverterContextFactory()
                .get(MessageConverterContextFactory.Resource.UNIPARC, MediaType.APPLICATION_XML);
    }

    @Override
    public AbstractEntityHttpMessageConverter<UniParcEntry> getXmlConverter() {
        return new UniParcXmlMessageConverter("2021_04");
    }

    @Override
    protected UniParcEntry getEntry() {
        return UniParcEntryMocker.createUniParcEntry(2, "UPI");
    }
}
