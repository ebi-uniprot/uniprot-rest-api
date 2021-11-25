package org.uniprot.api.uniparc.output.converter;

import static org.uniprot.api.rest.output.converter.ConverterConstants.COPYRIGHT_TAG;
import static org.uniprot.api.rest.output.converter.ConverterConstants.UNIPARC_XML_CLOSE_TAG;
import static org.uniprot.api.rest.output.converter.ConverterConstants.UNIPARC_XML_CONTEXT;
import static org.uniprot.api.rest.output.converter.ConverterConstants.UNIPARC_XML_SCHEMA;
import static org.uniprot.api.rest.output.converter.ConverterConstants.XML_DECLARATION;

import javax.xml.bind.Marshaller;

import org.uniprot.api.rest.output.converter.AbstractXmlMessageConverter;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.util.Utils;
import org.uniprot.core.xml.jaxb.uniparc.Entry;
import org.uniprot.core.xml.uniparc.UniParcEntryConverter;

/**
 * @author jluo
 * @date: 21 Jun 2019
 */
public class UniParcXmlMessageConverter extends AbstractXmlMessageConverter<UniParcEntry, Entry> {
    private final UniParcEntryConverter converter;

    private String header;

    public UniParcXmlMessageConverter(String version) {
        super(UniParcEntry.class, UNIPARC_XML_CONTEXT);
        converter = new UniParcEntryConverter();
        header = XML_DECLARATION + UNIPARC_XML_SCHEMA;
        if (Utils.notNullNotEmpty(version)) {
            String versionAttrib = " version=\"" + version + "\"" + ">\n";
            header = header.replace(">\n", versionAttrib);
        }
    }

    @Override
    protected String getHeader() {
        return header;
    }

    @Override
    protected Entry toXml(UniParcEntry entity) {

        return converter.toXml(entity);
    }

    @Override
    protected Marshaller getMarshaller() {
        return createMarshaller();
    }

    @Override
    protected String getFooter() {
        return COPYRIGHT_TAG + UNIPARC_XML_CLOSE_TAG;
    }
}
