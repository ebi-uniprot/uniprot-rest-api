package uk.ac.ebi.uniprot.uuw.advanced.search.http.converter;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.slf4j.Logger;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import com.sun.xml.bind.marshaller.DataWriter;

import uk.ac.ebi.kraken.ffwriter.line.impl.UniProtFlatfileWriter;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.kraken.xml.exception.UniProtXmlException;
import uk.ac.ebi.kraken.xml.jaxb.uniprot.Entry;
import uk.ac.ebi.uniprot.dataservice.restful.entry.EntryXmlConverter;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.EntryXmlConverterImpl;

public class UniProtXmlMessageConverter extends AbstractHttpMessageConverter<Stream<Collection<UniProtEntry>>> {
	private static final MediaType MEDIA_TYPE = MediaType.APPLICATION_XML;
	private static final int FLUSH_INTERVAL = 5000;
	private static final Logger LOGGER = getLogger(UniProtXmlMessageConverter.class);
	private EntryXmlConverter xmlConverter = new EntryXmlConverterImpl();
	private Marshaller marshaller;

	public UniProtXmlMessageConverter() {
		super(MEDIA_TYPE);
		marshaller = initXmlMarshaller();
	}

	private Marshaller initXmlMarshaller() {
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance("uk.ac.ebi.kraken.xml.jaxb.uniprot");
			Marshaller marshaller = jaxbContext.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
			return marshaller;
		} catch (Exception e) {
			throw new RuntimeException("JAXB initiallation failed", e);
		}
	}

	@Override
	protected boolean supports(Class<?> clazz) {
		return Stream.class.isAssignableFrom(clazz);
	}

	@Override
	protected Stream<Collection<UniProtEntry>> readInternal(Class<? extends Stream<Collection<UniProtEntry>>> clazz,
			HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void writeInternal(Stream<Collection<UniProtEntry>> contentStream, HttpOutputMessage httpOutputMessage)
			throws IOException, HttpMessageNotWritableException {
		AtomicInteger counter = new AtomicInteger();
		OutputStream outputStream = httpOutputMessage.getBody();
		Instant start = Instant.now();

		try {
			contentStream.forEach(items -> {
				items.forEach(entry -> {
					try {
						int currentCount = counter.getAndIncrement();
						if (currentCount % FLUSH_INTERVAL == 0) {
							outputStream.flush();
						}
						if (currentCount % 10000 == 0) {
							logStats(currentCount, start);
						}

						outputStream.write((getXmlString(entry)).getBytes());
					} catch (IOException e) {
						throw new StopStreamException("Could not write entry: " + entry, e);
					}
				});
			});

			logStats(counter.get(), start);
		} catch (StopStreamException e) {
			LOGGER.error("Client aborted streaming: closing stream.", e);
			contentStream.close();
		} finally {
			outputStream.flush();
		}
	}

	private String getXmlString(UniProtEntry uniProtEntry) {
		try {
			Entry entry = xmlConverter.convert(uniProtEntry);
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

	private void logStats(int counter, Instant start) {
		Instant now = Instant.now();
		long millisDuration = Duration.between(start, now).toMillis();
		int secDuration = (int) millisDuration / 1000;
		String rate = String.format("%.2f", ((double) counter) / secDuration);
		LOGGER.info("UniProt flatfile entries written: {}", counter);
		LOGGER.info("UniProt flatfile entries duration: {} ({} entries/sec)", secDuration, rate);
	}

}
