package uk.ac.ebi.uniprot.uuw.advanced.search.http.converter;

import com.sun.xml.bind.marshaller.DataWriter;
import org.slf4j.Logger;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import uk.ac.ebi.uniprot.uuw.advanced.search.http.context.XmlMessageConverterContext;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import java.io.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import static org.slf4j.LoggerFactory.getLogger;

public class XmlMessageConverter<S, T> extends AbstractHttpMessageConverter<XmlMessageConverterContext<S, T>> {
    private static final int FLUSH_INTERVAL = 5000;
    private static final Logger LOGGER = getLogger(UniProtXmlMessageConverter.class);
    public static final MediaType XML_MEDIA_TYPE = new MediaType("application", "xml");

    private final Map<String, Marshaller> marshallers;

    public XmlMessageConverter() {
        super(XML_MEDIA_TYPE);
        marshallers = new HashMap<>();
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return XmlMessageConverterContext.class.isAssignableFrom(clazz);
    }

    @Override
    protected XmlMessageConverterContext<S, T> readInternal(Class<? extends XmlMessageConverterContext<S, T>> aClass, HttpInputMessage httpInputMessage) throws IOException, HttpMessageNotReadableException {
        return null;
    }

    @Override
    protected void writeInternal(XmlMessageConverterContext<S, T> messageConfig, HttpOutputMessage httpOutputMessage)
            throws IOException, HttpMessageNotWritableException {
        AtomicInteger counter = new AtomicInteger();
        OutputStream outputStream = httpOutputMessage.getBody();

        if (messageConfig.isCompressed()) {
            try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream)) {
                writeDataToOutputStream(messageConfig, counter, gzipOutputStream);
            }
        } else {
            writeDataToOutputStream(messageConfig, counter, outputStream);
        }

    }

    private void writeDataToOutputStream(XmlMessageConverterContext<S, T> contentStream, AtomicInteger counter, OutputStream outputStream) throws IOException {
        initXmlMarshaller(contentStream);
        Instant start = Instant.now();
        final MutableWriteState writeState = new MutableWriteState();
        Stream<Collection<S>> entities = (Stream<Collection<S>>)contentStream.getEntities();

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

                        outputStream.write((getXmlString(contentStream, entry)).getBytes());
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

    private void initXmlMarshaller(XmlMessageConverterContext<S, T> config) {
        marshallers.putIfAbsent(config.getContext(), createMarshaller(config.getContext()));
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

    private String getXmlString(XmlMessageConverterContext<S, T> config, S uniProtEntry) {
        try {
            Function<S, T> converter = config.getConverter();
            T entry = converter.apply(uniProtEntry);
            StringWriter xmlString = new StringWriter();
            Writer out = new BufferedWriter(xmlString);
            DataWriter writer = new DataWriter(out, "UTF-8");
            writer.setIndentStep("  ");
            marshallers.get(config.getContext()).marshal(entry, writer);
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
