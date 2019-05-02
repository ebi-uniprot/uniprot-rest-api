package uk.ac.ebi.uniprot.api.uniprotkb.output.converter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import uk.ac.ebi.uniprot.api.rest.output.converter.AbstractXmlMessageConverter;
import uk.ac.ebi.uniprot.domain.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.xml.jaxb.uniprot.Entry;
import uk.ac.ebi.uniprot.xml.uniprot.UniProtEntryConverter;

public class UniProtKBXmlMessageConverter extends AbstractXmlMessageConverter<UniProtEntry, Entry> {
	private final UniProtEntryConverter converter;
	private final Marshaller marshaller;
	private static final String XML_CONTEXT = "uk.ac.ebi.uniprot.xml.jaxb.uniprot";

	public UniProtKBXmlMessageConverter() {
		converter = new UniProtEntryConverter();
		marshaller = createMarshaller(XML_CONTEXT);
	}

	private Marshaller createMarshaller(String context) {
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(context);
			Marshaller contextMarshaller = jaxbContext.createMarshaller();
			contextMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			contextMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
			return contextMarshaller;
		} catch (Exception e) {
			throw new RuntimeException("JAXB initialisation failed", e);
		}
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
