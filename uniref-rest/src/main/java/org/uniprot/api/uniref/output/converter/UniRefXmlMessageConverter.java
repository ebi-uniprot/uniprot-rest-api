package org.uniprot.api.uniref.output.converter;

import javax.xml.bind.Marshaller;

import org.uniprot.api.rest.output.converter.AbstractXmlMessageConverter;
import org.uniprot.core.uniref.UniRefEntry;
import org.uniprot.core.xml.jaxb.uniref.Entry;
import org.uniprot.core.xml.uniref.UniRefEntryConverter;

/**
 * @author jluo
 * @date: 22 Aug 2019
 */
public class UniRefXmlMessageConverter extends AbstractXmlMessageConverter<UniRefEntry, Entry> {
    private final UniRefEntryConverter converter;
    private static final String XML_CONTEXT = "org.uniprot.core.xml.jaxb.uniref";
    private static final String HEADER_PREFIX =
            "<UniRef xmlns=\"http://uniprot.org/uniref\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://uniprot.org/uniref http://www.uniprot.org/docs/uniref.xsd\"";

    private static final String FOOTER = "\n</UniRef>";

    private String header;

    public UniRefXmlMessageConverter(String version, String releaseDate) {
        super(UniRefEntry.class, XML_CONTEXT);
        converter = new UniRefEntryConverter();
        header = HEADER_PREFIX;
        if ((version != null) && (!version.isEmpty())) {
            header += " version=\"" + version + "\"";
        }

        if ((releaseDate != null) && (!releaseDate.isEmpty())) {
            header += " releaseDate=\"" + releaseDate + "\"";
        }
        header += ">\n";
    }

    @Override
    protected String getHeader() {
        return header;
    }

    @Override
    protected Entry toXml(UniRefEntry entity) {

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
