package uk.ac.ebi.uniprot.api.proteome.output.converter;

import javax.xml.bind.Marshaller;

import uk.ac.ebi.uniprot.api.rest.output.converter.AbstractXmlMessageConverter;
import uk.ac.ebi.uniprot.domain.proteome.CanonicalProtein;
import uk.ac.ebi.uniprot.xml.jaxb.proteome.CanonicalGene;
import uk.ac.ebi.uniprot.xml.proteome.CanonicalProteinConverter;

/**
 *
 * @author jluo
 * @date: 21 Jun 2019
 *
*/

public class GeneCentricXmlMessageConverter  extends AbstractXmlMessageConverter<CanonicalProtein, CanonicalGene> {
	private final CanonicalProteinConverter converter;
	private final Marshaller marshaller;
	private static final String XML_CONTEXT = "uk.ac.ebi.uniprot.xml.jaxb.proteome";
	private static final String HEADER = "<uniprot xmlns=\"http://uniprot.org/uniprot\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://uniprot.org/uniprot http://www.uniprot.org/support/docs/proteome.xsd\">\n";

	public GeneCentricXmlMessageConverter() {
		super(CanonicalProtein.class);
		converter = new CanonicalProteinConverter();
		marshaller = createMarshaller(XML_CONTEXT);
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
		return marshaller;
	}
}