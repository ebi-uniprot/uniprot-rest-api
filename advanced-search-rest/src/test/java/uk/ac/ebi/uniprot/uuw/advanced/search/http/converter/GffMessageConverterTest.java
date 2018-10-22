package uk.ac.ebi.uniprot.uuw.advanced.search.http.converter;

import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.EntryGffConverter;
import uk.ac.ebi.uniprot.uuw.advanced.search.http.context.MessageConverterContext;
import uk.ac.ebi.uniprot.uuw.advanced.search.mockers.UniProtEntryMocker;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.ac.ebi.uniprot.uuw.advanced.search.mockers.UniProtEntryMocker.Type.SP;

/**
 * 
 * @author gqi
 *
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class GffMessageConverterTest {
    @Mock
    private MessageConverterContext context;

    @Mock
    private OutputStream outputStream;

    private GffMessageConverter converter;
    private Instant start;
    private AtomicInteger count;

    @Before
    public void setUp() {
        this.converter = new GffMessageConverter();
        this.start = Instant.now();
        this.count = new AtomicInteger();
    }

    @Test
    public void convertsEntrySuccessfully() throws IOException {
        UniProtEntry entry = UniProtEntryMocker.create(SP);
        Stream<Collection<UniProtEntry>> entries = Stream.of(singletonList(entry));
        when((Stream<Collection<UniProtEntry>>) context.getEntities()).thenReturn(entries);

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

    private void doConversion() throws IOException {
        converter.write(context, outputStream, start, count);
    }

    private byte[] bytesFor(UniProtEntry entry) {
        String entryStr = EntryGffConverter.convert(entry) + "\n";
        System.out.println(entryStr);
        return entryStr.getBytes();
    }
}