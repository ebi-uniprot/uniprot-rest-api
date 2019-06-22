package uk.ac.ebi.uniprot.api.uniparc.output.converter;

import javax.xml.bind.Marshaller;

import uk.ac.ebi.uniprot.api.rest.output.converter.AbstractXmlMessageConverter;
import uk.ac.ebi.uniprot.domain.uniparc.UniParcEntry;
import uk.ac.ebi.uniprot.xml.jaxb.uniparc.Entry;
import uk.ac.ebi.uniprot.xml.uniparc.UniParcEntryConverter;

/**
 *
 * @author jluo
 * @date: 21 Jun 2019
 *
*/

public class UniParcXmlMessageConverter extends AbstractXmlMessageConverter<UniParcEntry, Entry> {
	private final UniParcEntryConverter converter;
	private final Marshaller marshaller;
	private static final String XML_CONTEXT = "uk.ac.ebi.uniprot.xml.jaxb.uniparc";
	private static final String HEADER = "<uniprot xmlns=\"http://uniprot.org/uniprot\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://uniprot.org/uniprot http://www.uniprot.org/support/docs/uniparc.xsd\">\n";

	public UniParcXmlMessageConverter() {
		super(UniParcEntry.class);
		converter = new UniParcEntryConverter();
		marshaller = createMarshaller(XML_CONTEXT);
	}

	@Override
	protected String getHeader() {
		return HEADER;
	}

	@Override
	protected Entry toXml(UniParcEntry entity) {
		
			return converter.toXml( entity);

	}

	@Override
	protected Marshaller getMarshaller() {
		return marshaller;
	}

}
