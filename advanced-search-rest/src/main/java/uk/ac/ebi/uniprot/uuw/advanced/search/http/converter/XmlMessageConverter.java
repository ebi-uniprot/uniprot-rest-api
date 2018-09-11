package uk.ac.ebi.uniprot.uuw.advanced.search.http.converter;

import com.sun.xml.bind.marshaller.DataWriter;
import org.slf4j.Logger;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import uk.ac.ebi.kraken.model.uniprot.UniProtEntryImpl;
import uk.ac.ebi.uniprot.uuw.advanced.search.http.context.XmlMessageConverterContext;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import java.io.*;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.slf4j.LoggerFactory.getLogger;

public class XmlMessageConverter<S, T> extends AbstractUUWHttpMessageConverter<XmlMessageConverterContext<S, T>> {
    private static final int FLUSH_INTERVAL = 5000;
    private static final Logger LOGGER = getLogger(XmlMessageConverter.class);
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
    protected void write(XmlMessageConverterContext<S, T> messageConfig,
                         OutputStream outputStream,
                         Instant start,
                         AtomicInteger counter) throws IOException {
        initXmlMarshaller(messageConfig);
        final MutableWriteState writeState = new MutableWriteState();
        Stream<Collection<S>> entities = (Stream<Collection<S>>)messageConfig.getEntities();

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

                        // try converting here so in case of exception, we abort before writing xml header
                        String entryXml = getXmlString(messageConfig, entry);

                        if (writeState.state == WriteState.CREATED) {
                            writeState.setWriteState(WriteState.ENTRIES_WRITTEN);
                            outputStream.write(messageConfig.getHeader().getBytes());
                        }

                        outputStream.write(entryXml.getBytes());
                    } catch (IOException | RuntimeException e) {
                        throw new StopStreamException("Could not write xml entry: " + ((UniProtEntryImpl)entry).getPrimaryUniProtAccession().getValue(), e);
                    }
                });
            });
        } catch (StopStreamException e) {
            LOGGER.error("Streaming aborted after error found. Closing stream.", e);
            entities.close();
        } finally {
            if (writeState.state == WriteState.ENTRIES_WRITTEN) {
                outputStream.write(messageConfig.getFooter().getBytes());
            }
            outputStream.close();
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
