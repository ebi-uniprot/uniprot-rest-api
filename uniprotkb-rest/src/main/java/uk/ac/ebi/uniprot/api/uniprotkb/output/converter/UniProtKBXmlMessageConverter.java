package uk.ac.ebi.uniprot.api.uniprotkb.output.converter;

import uk.ac.ebi.uniprot.api.rest.output.converter.AbstractXmlMessageConverter;
import uk.ac.ebi.uniprot.domain.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.xml.jaxb.uniprot.Entry;
import uk.ac.ebi.uniprot.xml.uniprot.UniProtEntryConverter;

import javax.xml.bind.Marshaller;

public class UniProtKBXmlMessageConverter extends AbstractXmlMessageConverter<UniProtEntry, Entry> {
	private final UniProtEntryConverter converter;
	private final Marshaller marshaller;
	private static final String XML_CONTEXT = "uk.ac.ebi.uniprot.xml.jaxb.uniprot";

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
