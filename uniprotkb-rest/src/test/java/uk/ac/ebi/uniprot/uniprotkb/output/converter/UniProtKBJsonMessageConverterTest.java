package uk.ac.ebi.uniprot.uniprotkb.output.converter;

import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.uniprot.common.repository.search.facet.Facet;
import uk.ac.ebi.uniprot.rest.output.context.MessageConverterContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * Created 08/11/18
 *
 * @author Edd
 */
public class UniProtKBJsonMessageConverterTest {
    private UniProtKBJsonMessageConverter converter;
    private ByteArrayOutputStream outputStream;

    @Before
    public void setUp() {
        converter = new UniProtKBJsonMessageConverter();
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    public void hasCorrectMediaType() {
        assertThat(converter.getSupportedMediaTypes(), is(singletonList(APPLICATION_JSON)));
    }

    @Test
    public void beforeWritesCorrectlyWithoutFacets() throws IOException {
        converter.before(MessageConverterContext.<String>builder()
                                 .entities(Stream.of("a", "b"))
                                 .build(), outputStream);
        flushWriter();

        assertThat()
        System.out.println(outputStream.toString());
    }

    @Test
    public void beforeWritesCorrectlyWithFacets() throws IOException {
        Facet facet1;
        Facet facet2;
        converter.before(MessageConverterContext.<String>builder()
                                 .entities(Stream.of("a", "b"))
                                 .facets(asList(facet1, facet2))
                                 .build(), outputStream);
        flushWriter();

        System.out.println(outputStream.toString());
    }

    private void flushWriter() {
        converter.cleanUp();
    }
}