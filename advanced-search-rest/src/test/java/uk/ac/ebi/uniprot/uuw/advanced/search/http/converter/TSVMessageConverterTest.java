package uk.ac.ebi.uniprot.uuw.advanced.search.http.converter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.uuw.advanced.search.http.context.MessageConverterContext;
import uk.ac.ebi.uniprot.uuw.advanced.search.mockers.UniProtEntryMocker;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.request.SearchRequestDTO;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static uk.ac.ebi.uniprot.uuw.advanced.search.mockers.UniProtEntryMocker.Type.SP;

/**
 * Created 16/10/18
 *
 * @author Edd
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class TSVMessageConverterTest {
    public static final String FIELDS = "accession,id";
    @Mock
    private MessageConverterContext context;

    @Mock
    private OutputStream outputStream;

    private TSVMessageConverter converter;
    private Instant start;
    private AtomicInteger count;
    private SearchRequestDTO searchRequest;

    @Before
    public void setUp() {
        this.converter = new TSVMessageConverter();
        this.start = Instant.now();
        this.count = new AtomicInteger();
        this.searchRequest = new SearchRequestDTO();
    }

    @Test
    public void convertsEntrySuccessfully() throws IOException {
        UniProtEntry entry = UniProtEntryMocker.create(SP);
        Stream<Collection<UniProtEntry>> entries = Stream.of(singletonList(entry));
        when((Stream<Collection<UniProtEntry>>) context.getEntities()).thenReturn(entries);
        searchRequest.setFields(FIELDS);
        when(context.getRequestDTO()).thenReturn(searchRequest);

        doConversion();

        verify(outputStream).write(headerFor());
        verify(outputStream).write(tsvBytesFor(entry));
        verify(outputStream).close();
    }

    private byte[] headerFor() {
        return "Entry\tEntry Name\n".getBytes();
    }

    @Test
    public void errorDuringWriteHeaderCausesStreamClosure() throws IOException {
        UniProtEntry entry = UniProtEntryMocker.create(SP);
        Stream<Collection<UniProtEntry>> entries = Stream.of(singletonList(entry));
        when((Stream<Collection<UniProtEntry>>) context.getEntities()).thenReturn(entries);
        searchRequest.setFields(FIELDS);
        when(context.getRequestDTO()).thenReturn(searchRequest);

        doThrow(IOException.class).when(outputStream).write(any());

        doConversion();

        verify(outputStream).close();
    }

    @Test
    public void errorDuringWriteCausesStreamClosure() throws IOException {
        UniProtEntry entry = UniProtEntryMocker.create(SP);
        Stream<Collection<UniProtEntry>> entries = Stream.of(singletonList(entry));
        when((Stream<Collection<UniProtEntry>>) context.getEntities()).thenReturn(entries);
        searchRequest.setFields(FIELDS);
        when(context.getRequestDTO()).thenReturn(searchRequest);

        doThrow(Error.class).when(outputStream).write(tsvBytesFor(entry));

        doConversion();

        verify(outputStream).close();
    }

    private void doConversion() throws IOException {
        converter.write(context, outputStream, start, count);
    }

    private byte[] tsvBytesFor(UniProtEntry entry) {
        List<String> values = asList(entry.getPrimaryUniProtAccession().getValue(), entry.getUniProtId().getValue());
        return values.stream().collect(Collectors.joining("\t", "", "\n")).getBytes();
    }
}