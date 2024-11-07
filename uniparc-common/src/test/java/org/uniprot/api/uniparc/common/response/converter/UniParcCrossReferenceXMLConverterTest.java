package org.uniprot.api.uniparc.common.response.converter;

import org.springframework.http.MediaType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.output.converter.AbstractEntityHttpMessageConverter;
import org.uniprot.api.rest.output.converter.AbstractXmlValidationTest;
import org.uniprot.api.uniparc.common.response.UniParcMessageConverterConfig;
import org.uniprot.core.uniparc.UniParcCrossReference;
import org.uniprot.store.indexer.uniparc.mockers.UniParcCrossReferenceMocker;

public class UniParcCrossReferenceXMLConverterTest
        extends AbstractXmlValidationTest<UniParcCrossReference> {

    @Override
    public String getXSDUrlLocation() {
        return "https://www.uniprot.org/docs/uniparc-dbreference.xsd";
    }

    @Override
    public MessageConverterContext<UniParcCrossReference> getMessageConverterConfig() {
        return new UniParcMessageConverterConfig()
                .uniParcCrossReferenceMessageConverterContextFactory()
                .get(MessageConverterContextFactory.Resource.CROSSREF, MediaType.APPLICATION_XML);
    }

    @Override
    public AbstractEntityHttpMessageConverter<UniParcCrossReference> getXmlConverter() {
        return new UniParcCrossReferenceXMLConverter();
    }

    @Override
    protected UniParcCrossReference getEntry() {
        return UniParcCrossReferenceMocker.createCrossReferences(2, 1).get(0);
    }
}
