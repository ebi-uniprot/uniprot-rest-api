package org.uniprot.api.uniparc.common.response.converter;

import java.io.IOException;
import java.io.OutputStream;

import javax.xml.bind.Marshaller;

import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.converter.AbstractXmlMessageConverter;
import org.uniprot.api.rest.output.converter.ConverterConstants;
import org.uniprot.core.uniparc.UniParcCrossReference;
import org.uniprot.core.xml.dbreference.UniParcCrossReferenceConverter;
import org.uniprot.core.xml.jaxb.dbreference.DbReference;

public class UniParcCrossReferenceXMLConverter
        extends AbstractXmlMessageConverter<UniParcCrossReference, DbReference> {
    private String header;
    private final ThreadLocal<UniParcCrossReferenceConverter> XML_CONVERTER = new ThreadLocal<>();

    public UniParcCrossReferenceXMLConverter() {
        this(null);
    }

    public UniParcCrossReferenceXMLConverter(Gatekeeper downloadGatekeeper) {
        super(
                UniParcCrossReference.class,
                ConverterConstants.UNIPARC_CROSS_REFERENCE_XML_CONTEXT,
                downloadGatekeeper);
        header = ConverterConstants.UNIPARC_CROSS_REFERENCE_XML_SCHEMA;
        header = ConverterConstants.XML_DECLARATION + header;
    }

    @Override
    protected DbReference toXml(UniParcCrossReference entity) {
        return XML_CONVERTER.get().toXml(entity);
    }

    @Override
    protected Marshaller getMarshaller() {
        return createMarshaller();
    }

    @Override
    protected String getFooter() {
        return ConverterConstants.UNIPARC_CROSS_REFERENCE_XML_CLOSE_TAG;
    }

    @Override
    protected String getHeader() {
        return this.header;
    }

    @Override
    protected void before(
            MessageConverterContext<UniParcCrossReference> context, OutputStream outputStream)
            throws IOException {
        super.before(context, outputStream);
        XML_CONVERTER.set(new UniParcCrossReferenceConverter());
    }

    @Override
    protected void cleanUp() {
        super.cleanUp();
        XML_CONVERTER.remove();
    }
}
