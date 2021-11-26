package org.uniprot.api.uniref.output.converter;

import javax.xml.bind.Marshaller;

import org.uniprot.api.rest.output.converter.AbstractXmlMessageConverter;
import org.uniprot.api.rest.output.converter.ConverterConstants;
import org.uniprot.core.uniref.UniRefEntry;
import org.uniprot.core.xml.jaxb.uniref.Entry;
import org.uniprot.core.xml.uniref.UniRefEntryConverter;

/**
 * @author jluo
 * @date: 22 Aug 2019
 */
public class UniRefXmlMessageConverter extends AbstractXmlMessageConverter<UniRefEntry, Entry> {
    private final UniRefEntryConverter converter;
    private String header;

    public UniRefXmlMessageConverter(String version, String releaseDate) {
        super(UniRefEntry.class, ConverterConstants.UNIREF_XML_CONTEXT);
        converter = new UniRefEntryConverter();
        header = ConverterConstants.UNIREF_XML_SCHEMA;
        if ((version != null) && (!version.isEmpty())) {
            header += " version=\"" + version + "\"";
        }

        if ((releaseDate != null) && (!releaseDate.isEmpty())) {
            header += " releaseDate=\"" + releaseDate + "\"";
        }
        header += ">\n";

        header = ConverterConstants.XML_DECLARATION + header;
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
    protected String getFooter() { // do not add copyright tag see TRM-27009
        return ConverterConstants.UNIREF_XML_CLOSE_TAG;
    }
}
