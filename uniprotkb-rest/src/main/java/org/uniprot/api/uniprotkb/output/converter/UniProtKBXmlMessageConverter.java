package org.uniprot.api.uniprotkb.output.converter;

import javax.xml.bind.Marshaller;

import org.uniprot.api.rest.output.converter.AbstractXmlMessageConverter;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.xml.jaxb.uniprot.Entry;
import org.uniprot.core.xml.uniprot.UniProtEntryConverter;

public class UniProtKBXmlMessageConverter
        extends AbstractXmlMessageConverter<UniProtKBEntry, Entry> {
    private final UniProtEntryConverter converter;
    private final Marshaller marshaller;
    private static final String XML_CONTEXT = "org.uniprot.core.xml.jaxb.uniprot";

    public static final String HEADER =
            "<uniprot xmlns=\"https://uniprot.org/uniprot\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"https://uniprot.org/uniprot https://www.uniprot.org/docs/uniprot.xsd\">\n";
    public static final String FOOTER =
            "<copyright>\n"
                    + "Copyrighted by the UniProt Consortium, see https://www.uniprot.org/terms Distributed under the Creative Commons Attribution (CC BY 4.0) License\n"
                    + "</copyright>\n"
                    + "</uniprot>";

    public UniProtKBXmlMessageConverter() {
        super(UniProtKBEntry.class);
        converter = new UniProtEntryConverter();
        marshaller = createMarshaller(XML_CONTEXT);
    }

    @Override
    protected Entry toXml(UniProtKBEntry entity) {
        return converter.toXml(entity);
    }

    @Override
    protected Marshaller getMarshaller() {
        return marshaller;
    }

    @Override
    protected String getFooter() {
        return FOOTER;
    }

    @Override
    protected String getHeader() {
        return HEADER;
    }
}
