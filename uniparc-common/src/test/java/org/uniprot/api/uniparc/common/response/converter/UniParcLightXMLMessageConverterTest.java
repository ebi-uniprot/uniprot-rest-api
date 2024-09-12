package org.uniprot.api.uniparc.common.response.converter;

import org.springframework.http.MediaType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.output.converter.AbstractEntityHttpMessageConverter;
import org.uniprot.api.rest.output.converter.AbstractXmlValidationTest;
import org.uniprot.api.uniparc.common.response.UniParcMessageConverterConfig;
import org.uniprot.core.uniparc.UniParcEntryLight;
import org.uniprot.store.indexer.uniparc.mockers.UniParcEntryMocker;

class UniParcLightXMLMessageConverterTest extends AbstractXmlValidationTest<UniParcEntryLight> {

    @Override
    public String getXSDUrlLocation() {
        return "https://www.uniprot.org/docs/uniparc.xsd";
    }

    @Override
    public MessageConverterContext<UniParcEntryLight> getMessageConverterConfig() {
        return new UniParcMessageConverterConfig()
                .uniparcLightMessageConverterContextFactory()
                .get(MessageConverterContextFactory.Resource.UNIPARC, MediaType.APPLICATION_XML);
    }

    @Override
    public AbstractEntityHttpMessageConverter<UniParcEntryLight> getXmlConverter() {
        return new UniParcLightXMLMessageConverter("2021_04");
    }

    @Override
    protected UniParcEntryLight getEntry() {
        return UniParcEntryMocker.createUniParcEntryLight(2, "UPI", 3);
    }
}
