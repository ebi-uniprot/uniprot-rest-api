package uk.ac.ebi.uniprot.uuw.advanced.search.http.converter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.ac.ebi.uniprot.uuw.advanced.search.http.context.MessageConverterContext;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Created 21/09/18
 *
 * @author Edd
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ListMessageConverterTest {

    @Mock
    private MessageConverterContext context;

    @Mock
    private OutputStream outputStream;

    private ListMessageConverter converter;
    private Instant start;
    private AtomicInteger count;

    @Before
    public void setUp() {
        this.converter = new ListMessageConverter();
        this.start = Instant.now();
        this.count = new AtomicInteger();
    }

    @Test
    public void convertsEntrySuccessfully() throws IOException {
        String entry = "P12345";
        Stream<String> entries = Stream.of(entry);
//        when((Stream<String>)context.getEntities()).thenReturn(entries);

        doConversion();

        verify(outputStream).write(bytesFor(entry));
        verify(outputStream).close();
    }

    @Test
    public void errorDuringWriteCausesStreamClosure() throws IOException {
        doThrow(Error.class).when(outputStream).write(any());

        doConversion();

        verify(outputStream).close();
    }

    private byte[] bytesFor(String entry) {
        return (entry + "\n").getBytes();
    }

    private void doConversion() throws IOException {
        converter.write(context, outputStream, start, count);
    }
}