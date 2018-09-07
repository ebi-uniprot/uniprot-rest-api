package uk.ac.ebi.uniprot.uuw.advanced.search.http.converter;

import com.sun.xml.bind.marshaller.DataWriter;
import org.slf4j.Logger;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.kraken.xml.jaxb.uniprot.Entry;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import java.io.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import static org.slf4j.LoggerFactory.getLogger;

public class XmlMessageConverter extends AbstractHttpMessageConverter<XmlEntityMessageConverter> {
    private static final int FLUSH_INTERVAL = 5000;
    private static final Logger LOGGER = getLogger(UniProtXmlMessageConverter.class);
    private static final String HEADER = "<uniprot xmlns=\"http://uniprot.org/uniprot\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://uniprot.org/uniprot http://www.uniprot.org/support/docs/uniprot.xsd\">\n";
    private static final String FOOTER = "<copyright>\n" +
            "Copyrighted by the UniProt Consortium, see https://www.uniprot.org/terms Distributed under the Creative Commons Attribution (CC BY 4.0) License\n" +
            "</copyright>\n" +
            "</uniprot>";
    //    private EntryXmlConverter xmlConverter = new EntryXmlConverterImpl();
    private Marshaller marshaller;

    public XmlMessageConverter() {
        super(new MediaType("x-uniprot2", "xml"));
        marshaller = initXmlMarshaller();
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return XmlEntityMessageConverter.class.isAssignableFrom(clazz);
    }

    @Override
    protected XmlEntityMessageConverter readInternal(Class<? extends XmlEntityMessageConverter> aClass, HttpInputMessage httpInputMessage) throws IOException, HttpMessageNotReadableException {
        return null;
    }


    @Override
    protected void writeInternal(XmlEntityMessageConverter contentStream, HttpOutputMessage httpOutputMessage)
            throws IOException, HttpMessageNotWritableException {
        AtomicInteger counter = new AtomicInteger();
        OutputStream outputStream = httpOutputMessage.getBody();

        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream)) {
            writeDataToOutputStream(contentStream, counter, gzipOutputStream);
        }

    }

    private void writeDataToOutputStream(XmlEntityMessageConverter contentStream, AtomicInteger counter, OutputStream outputStream) throws IOException {
        Instant start = Instant.now();
        final MutableWriteState writeState = new MutableWriteState();
        Stream<Collection<UniProtEntry>> entities = contentStream.getEntities();

        try {
            entities.forEach(batch -> {
                batch.forEach(entry -> {
                    try {
                        int currentCount = counter.getAndIncrement();
                        if (currentCount % FLUSH_INTERVAL == 0) {
                            outputStream.flush();
                        }
                        if (currentCount % 10000 == 0) {
                            logStats(currentCount, start);
                        }
                        if (writeState.state == WriteState.CREATED) {
                            writeState.setWriteState(WriteState.ENTRIES_WRITTEN);
                            outputStream.write(contentStream.getHeader().getBytes());
                        }

                        outputStream.write((getXmlString(contentStream.getConverter(), entry)).getBytes());
                    } catch (IOException | RuntimeException e) {
                        throw new StopStreamException("Could not write xml entry: " + entry, e);
                    }
                });
            });

            logStats(counter.get(), start);
        } catch (StopStreamException e) {
            LOGGER.error("Streaming aborted after error found. Closing stream.", e);
            entities.close();
        } finally {
            if (writeState.state == WriteState.ENTRIES_WRITTEN) {
                outputStream.write(contentStream.getFooter().getBytes());
            }

            outputStream.flush();
        }
    }

    private Marshaller initXmlMarshaller() {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance("uk.ac.ebi.kraken.xml.jaxb.uniprot");
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            return marshaller;
        } catch (Exception e) {
            throw new RuntimeException("JAXB initialisation failed", e);
        }
    }

    private String getXmlString(Function<UniProtEntry, Entry> converter, UniProtEntry uniProtEntry) {
        try {
            Entry entry = converter.apply(uniProtEntry);
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
        LOGGER.info("UniProt xml entries written: {}", counter);
        LOGGER.info("UniProt xml entries duration: {} ({} entries/sec)", secDuration, rate);
    }

    private enum WriteState {
        CREATED, ENTRIES_WRITTEN
    }

    private static class MutableWriteState {
        private WriteState state;

        MutableWriteState() {
            this.state = WriteState.CREATED;
        }

        void setWriteState(WriteState state) {
            this.state = state;
        }
    }
}
