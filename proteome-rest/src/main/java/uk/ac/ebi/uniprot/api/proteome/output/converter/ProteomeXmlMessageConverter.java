package uk.ac.ebi.uniprot.api.proteome.output.converter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import uk.ac.ebi.uniprot.api.rest.output.converter.AbstractXmlMessageConverter;
import uk.ac.ebi.uniprot.domain.proteome.ProteomeEntry;
import uk.ac.ebi.uniprot.xml.jaxb.proteome.Proteome;
import uk.ac.ebi.uniprot.xml.proteome.ProteomeConverter;

/**
 *
 * @author jluo
 * @date: 29 Apr 2019
 *
 */

public class ProteomeXmlMessageConverter extends AbstractXmlMessageConverter<ProteomeEntry, Proteome> {
	private final ProteomeConverter converter;
	private final Marshaller marshaller;
	private static final String XML_CONTEXT = "uk.ac.ebi.uniprot.xml.jaxb.proteome";
	private static final String HEADER = "<uniprot xmlns=\"http://uniprot.org/uniprot\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://uniprot.org/uniprot http://www.uniprot.org/support/docs/proteome.xsd\">\n";

	public ProteomeXmlMessageConverter() {
		converter = new ProteomeConverter();
		marshaller = createMarshaller(XML_CONTEXT);
	}

	@Override
	protected String getHeader() {
		return HEADER;
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
	protected Proteome toXml(ProteomeEntry entity) {
		return converter.toXml(entity);
	}

	@Override
	protected Marshaller getMarshaller() {
		return marshaller;
	}

}
