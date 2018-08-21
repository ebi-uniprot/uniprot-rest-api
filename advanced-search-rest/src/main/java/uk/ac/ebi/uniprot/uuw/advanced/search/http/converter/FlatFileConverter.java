package uk.ac.ebi.uniprot.uuw.advanced.search.http.converter;

import org.slf4j.Logger;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created 21/08/18
 *
 * @author Edd
 */
public class FlatFileConverter extends AbstractHttpMessageConverter<Stream<String>> {
    private static final Logger LOGGER = getLogger(FlatFileConverter.class);
    private static final MediaType MEDIA_TYPE = new MediaType("text", "flatfile");
    public static final int FLUSH_INTERVAL = 10000;

    public FlatFileConverter() {
        super(MEDIA_TYPE);
    }

    @Override
    protected boolean supports(Class<?> aClass) {
        return Stream.class.isAssignableFrom(aClass);
    }

    @Override
    protected Stream<String> readInternal(Class<? extends Stream<String>> aClass, HttpInputMessage httpInputMessage) throws IOException, HttpMessageNotReadableException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void writeInternal(Stream<String> contentStream, HttpOutputMessage httpOutputMessage) throws IOException, HttpMessageNotWritableException {
        AtomicInteger counter = new AtomicInteger();

        OutputStream outputStream = httpOutputMessage.getBody();
        outputStream.write("(this will be a custom flatfile writer)!\n".getBytes());
        try {
            contentStream.forEach(
                    item -> {
                        try {
                            int currentCount = counter.getAndIncrement();
                            if (currentCount % FLUSH_INTERVAL == 0) {
                                outputStream.flush();
                            }
                            outputStream.write((item + "\n").getBytes());
                        } catch (IOException e) {
                            throw new StopStreamException("Could not write content: " + item, e);
                        }
                    }
            );
        } catch (StopStreamException e) {
            LOGGER.error("Client aborted streaming: closing stream.", e);
            outputStream.flush();
            contentStream.close();
        }
    }
}
