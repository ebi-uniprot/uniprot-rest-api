package uk.ac.ebi.uniprot.uuw.advanced.search.http.converter;

import org.apache.avro.generic.GenericData;
import org.slf4j.Logger;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import uk.ac.ebi.kraken.ffwriter.line.impl.UniProtFlatfileWriter;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.dataservice.voldemort.client.UniProtClient;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created 21/08/18
 *
 * @author Edd
 */
public class FlatFileConverter extends AbstractHttpMessageConverter<Stream<String>> {
    private static final Logger LOGGER = getLogger(FlatFileConverter.class);
    private static final MediaType MEDIA_TYPE = new MediaType("text", "flatfile");
//    private static final int FLUSH_INTERVAL = 10000;
    private static final int FLUSH_INTERVAL = 100;
    private final UniProtClient storeClient;

    public FlatFileConverter(UniProtClient uniProtClient) {
        super(MEDIA_TYPE);
        this.storeClient = uniProtClient;
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
            List<String> entryBuffer = new ArrayList<>();
            contentStream.forEach(
                    item -> {

                        try {
                            int currentCount = counter.getAndIncrement();

                            if (currentCount % 100 == 0) {
                                List<UniProtEntry> entries = storeClient.getEntries(entryBuffer);
                                entries.forEach(entry -> {
                                    try {
                                        outputStream.write(UniProtFlatfileWriter.write(entry).getBytes());
                                    } catch (IOException e) {
                                        throw new StopStreamException("Could not write content: " + item, e);
                                    }
                                });
                            }
                            entryBuffer.add(item);

                            if (currentCount % FLUSH_INTERVAL == 0) {
                                outputStream.flush();
                            }
//                            List<UniProtEntry> entries = storeClient.getEntries(singletonList(item));
//                            entries.forEach(entry -> {
//                                try {
//                                    outputStream.write(UniProtFlatfileWriter.write(entry).getBytes());
//                                } catch (IOException e) {
//                                    throw new StopStreamException("Could not write content: " + item, e);
//                                }
//                            });
//                            outputStream.write((item + "\n").getBytes());
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
