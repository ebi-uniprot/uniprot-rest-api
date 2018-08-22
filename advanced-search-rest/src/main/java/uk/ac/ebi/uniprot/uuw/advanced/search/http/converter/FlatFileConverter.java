package uk.ac.ebi.uniprot.uuw.advanced.search.http.converter;

import org.slf4j.Logger;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import uk.ac.ebi.kraken.ffwriter.line.impl.UniProtFlatfileWriter;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created 21/08/18
 *
 * @author Edd
 */
public class FlatFileConverter extends AbstractHttpMessageConverter<Stream<Collection<UniProtEntry>>> {
    private static final Logger LOGGER = getLogger(FlatFileConverter.class);
    private static final MediaType MEDIA_TYPE = new MediaType("text", "flatfile");
    private static final int FLUSH_INTERVAL = 5000;

    public FlatFileConverter() {
        super(MEDIA_TYPE);
    }

    @Override
    protected boolean supports(Class<?> aClass) {
        return Stream.class.isAssignableFrom(aClass);
    }

    @Override
    protected Stream<Collection<UniProtEntry>> readInternal(Class<? extends Stream<Collection<UniProtEntry>>> aClass, HttpInputMessage httpInputMessage) throws IOException, HttpMessageNotReadableException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void writeInternal(Stream<Collection<UniProtEntry>> contentStream, HttpOutputMessage httpOutputMessage) throws IOException, HttpMessageNotWritableException {
        AtomicInteger counter = new AtomicInteger();
        OutputStream outputStream = httpOutputMessage.getBody();

        try {
            contentStream.forEach(items -> {
                items.forEach(entry -> {
                    try {
                        int currentCount = counter.getAndIncrement();
                        if (currentCount % FLUSH_INTERVAL == 0) {
                            outputStream.flush();
                        }
                        if (currentCount % 10000 == 0) {
                            LOGGER.debug("UniProt flatfile entries written: {}", currentCount);
                        }

                        outputStream.write(UniProtFlatfileWriter.write(entry).getBytes());
                    } catch (IOException e) {
                        throw new StopStreamException("Could not write entry: " + entry, e);
                    }
                });
            });
        } catch (StopStreamException e) {
            LOGGER.error("Client aborted streaming: closing stream.", e);
            outputStream.flush();
            contentStream.close();
        }
    }
}
