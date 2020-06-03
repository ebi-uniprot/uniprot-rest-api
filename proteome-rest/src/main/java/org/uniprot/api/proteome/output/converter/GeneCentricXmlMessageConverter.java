package org.uniprot.api.proteome.output.converter;

import javax.xml.bind.Marshaller;

import org.uniprot.api.rest.output.converter.AbstractXmlMessageConverter;
import org.uniprot.core.proteome.CanonicalProtein;
import org.uniprot.core.xml.jaxb.proteome.CanonicalGene;
import org.uniprot.core.xml.proteome.CanonicalProteinConverter;

/**
 * @author jluo
 * @date: 21 Jun 2019
 */
public class GeneCentricXmlMessageConverter
        extends AbstractXmlMessageConverter<CanonicalProtein, CanonicalGene> {
    private final CanonicalProteinConverter converter;
    private static final String XML_CONTEXT = "org.uniprot.core.xml.jaxb.proteome";
    private static final String HEADER =
            "<proteomes xmlns=\"https://uniprot.org/uniprot\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"https://uniprot.org/uniprot https://www.uniprot.org/docs/proteome.xsd\">\n";

    private static final String FOOTER = "\n</proteomes>";

    public GeneCentricXmlMessageConverter() {
        super(CanonicalProtein.class, XML_CONTEXT);
        converter = new CanonicalProteinConverter();
    }

    @Override
    protected String getHeader() {
        return HEADER;
    }

    @Override
    protected CanonicalGene toXml(CanonicalProtein entity) {
        return converter.toXml(entity);
    }

    @Override
    protected Marshaller getMarshaller() {
        return createMarshaller();
    }

    @Override
    protected String getFooter() {
        return FOOTER;
    }
}
