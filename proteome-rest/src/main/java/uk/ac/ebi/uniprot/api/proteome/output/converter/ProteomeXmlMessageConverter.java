package uk.ac.ebi.uniprot.api.proteome.output.converter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.springframework.http.MediaType;

import com.sun.xml.bind.marshaller.DataWriter;

import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContext;
import uk.ac.ebi.uniprot.api.rest.output.converter.AbstractEntityHttpMessageConverter;
import uk.ac.ebi.uniprot.domain.proteome.ProteomeEntry;
import uk.ac.ebi.uniprot.xml.jaxb.proteome.Proteome;
import uk.ac.ebi.uniprot.xml.proteome.ProteomeConverter;

/**
 *
 * @author jluo
 * @date: 29 Apr 2019
 *
*/

public class ProteomeXmlMessageConverter extends AbstractEntityHttpMessageConverter<ProteomeEntry> {
	  private final ProteomeConverter converter;
	    private final Marshaller marshaller;
	    private static final String XML_CONTEXT = "uk.ac.ebi.uniprot.xml.jaxb.proteome";
	    private static final String HEADER = "<uniprot xmlns=\"http://uniprot.org/uniprot\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://uniprot.org/uniprot http://www.uniprot.org/support/docs/proteome.xsd\">\n";
	    private static final String FOOTER = "<copyright>\n" +
	                "Copyrighted by the UniProt Consortium, see https://www.uniprot.org/terms Distributed under the Creative Commons Attribution (CC BY 4.0) License\n" +
	                "</copyright>\n" +
	                "</uniprot>";

	    public ProteomeXmlMessageConverter() {
	        super(MediaType.APPLICATION_XML);
	        converter = new ProteomeConverter();
	        marshaller = createMarshaller(XML_CONTEXT);
	    }
	    

	    @Override
	    protected void writeEntity(ProteomeEntry entity, OutputStream outputStream) throws IOException {
	        outputStream.write(getXmlString(entity).getBytes());
	    }

	    @Override
	    protected void after(MessageConverterContext<ProteomeEntry> context, OutputStream outputStream) throws IOException {
	        outputStream.write(FOOTER.getBytes());
	    }

	    @Override
	    protected void before(MessageConverterContext<ProteomeEntry> context, OutputStream outputStream) throws IOException {
	        outputStream.write(HEADER.getBytes());
	    }

	    String getXmlString(ProteomeEntry proteomeEntry) {
	        try {
	        	Proteome entry = converter.toXml(proteomeEntry);
	            StringWriter xmlString = new StringWriter();
	            Writer out = new BufferedWriter(xmlString);
	            DataWriter writer = new DataWriter(out, "UTF-8");
	            writer.setIndentStep("  ");
	            marshaller.marshal(entry, writer);
	            writer.characters("\n");
	            writer.flush();
	            return xmlString.toString();
	        } catch (Exception e) {
	            throw new RuntimeException(e);
	        }
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

}

