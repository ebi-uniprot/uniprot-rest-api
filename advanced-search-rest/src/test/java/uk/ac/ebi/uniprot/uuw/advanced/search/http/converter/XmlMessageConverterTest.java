package uk.ac.ebi.uniprot.uuw.advanced.search.http.converter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.ac.ebi.uniprot.uuw.advanced.search.http.context.XmlMessageConverterContext;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static uk.ac.ebi.uniprot.uuw.advanced.search.http.converter.XmlMessageConverter.XML_MEDIA_TYPE;
import static uk.ac.ebi.uniprot.uuw.advanced.search.http.converter.XmlMessageConverterTest.FakeSource.createFakeSource;

/**
 * Created 27/09/18
 *
 * @author Edd
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class XmlMessageConverterTest {
    private static final String HEADER = "<header>";
    private static final String FOOTER = "<footer>";
    private static final String XML_STRING = "<fake-xml/>";

    @Mock
    private XmlMessageConverterContext<FakeSource, FakeXMLEntity> context;

    @Mock
    private OutputStream outputStream;

    private AtomicInteger count;
    private Instant start;
    private TestableXmlMessageConverter converter;

    @Before
    public void setUp() {
        this.converter = new TestableXmlMessageConverter();
        this.start = Instant.now();
        this.count = new AtomicInteger();
        mockXmlMessageConverterContext();
    }

    @Test
    public void convertsEntrySuccessfully() throws IOException {
        Stream<Collection<FakeSource>> entries = Stream.of(singletonList(createFakeSource(1)));
        when((Stream<Collection<FakeSource>>) context.getEntities()).thenReturn(entries);

        doConversion();

        verify(outputStream).write(HEADER.getBytes());
        verify(outputStream).write(XML_STRING.getBytes());
        verify(outputStream).write(FOOTER.getBytes());
        verify(outputStream).close();
    }

    @Test
    public void errorDuringWriteCausesStreamClosure() throws IOException {
        when((Stream<Collection<FakeSource>>) context.getEntities()).thenReturn(Stream.of(singletonList(createFakeSource(1))));
        doThrow(Error.class).when(outputStream).write(any());

        doConversion();

        verify(outputStream).close();
    }

    private void mockXmlMessageConverterContext() {
        when(context.getHeader()).thenReturn(HEADER);
        when(context.getFooter()).thenReturn(FOOTER);
        when(context.getContentType()).thenReturn(XML_MEDIA_TYPE);
    }

    private void doConversion() throws IOException {
        converter.write(context, outputStream, start, count);
    }

    private static class TestableXmlMessageConverter extends XmlMessageConverter<FakeSource, FakeXMLEntity> {
        @Override
        String getXmlString(XmlMessageConverterContext<FakeSource, FakeXMLEntity> config, FakeSource entry) {
            return XML_STRING;
        }

        @Override
        void initXmlMarshaller(XmlMessageConverterContext<FakeSource, FakeXMLEntity> config) {
            // no-op
        }
    }

    static class FakeSource {
        int id;

        static FakeSource createFakeSource(int id) {
            FakeSource entity = new FakeSource();
            entity.id = id;
            return entity;
        }
    }

    static class FakeXMLEntity {}
}
