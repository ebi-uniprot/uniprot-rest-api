package org.uniprot.api.uniprotkb.output.converter;

import javax.xml.bind.Marshaller;

import org.uniprot.api.rest.output.converter.AbstractXmlMessageConverter;
import org.uniprot.core.uniprot.UniProtEntry;
import org.uniprot.core.xml.jaxb.uniprot.Entry;
import org.uniprot.core.xml.uniprot.UniProtEntryConverter;

public class UniProtKBXmlMessageConverter extends AbstractXmlMessageConverter<UniProtEntry, Entry> {
	private final UniProtEntryConverter converter;
	private final Marshaller marshaller;
	private static final String XML_CONTEXT = "org.uniprot.core.xml.jaxb.uniprot";

	public UniProtKBXmlMessageConverter() {
        super(UniProtEntry.class);
		converter = new UniProtEntryConverter();
		marshaller = createMarshaller(XML_CONTEXT);
	}

	@Override
	protected Entry toXml(UniProtEntry entity) {
		return converter.toXml(entity);
	}

	@Override
	protected Marshaller getMarshaller() {
		return marshaller;
	}
}
