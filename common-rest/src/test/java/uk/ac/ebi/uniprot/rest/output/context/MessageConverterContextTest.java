package uk.ac.ebi.uniprot.rest.output.context;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import uk.ac.ebi.uniprot.common.repository.search.facet.Facet;

import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.ac.ebi.uniprot.rest.output.context.MessageConverterContext.DEFAULT_FILE_TYPE;

/**
 * Created 08/11/18
 *
 * @author Edd
 */
class MessageConverterContextTest {
    @Test
    void canCreateUsingBuilder() {
        FileType fileType = FileType.FILE;
        MediaType contentType = MediaType.APPLICATION_JSON;
        Stream<String> entityStream = Stream.of("a", "b");
        Stream<String> entityIdStream = Stream.of("a.id", "b.id");
        List<Facet> facets = emptyList();
        MessageConverterContextFactory.Resource resource = MessageConverterContextFactory.Resource.UNIPROT;
        String fields = "field1,field2";
        
        MessageConverterContext<String> context = MessageConverterContext.<String>builder()
                .fileType(fileType)
                .contentType(contentType)
                .entities(entityStream)
                .entityIds(entityIdStream)
                .facets(facets)
                .fields(fields)
                .resource(resource)
                .build();

        assertThat(context.getContentType(), is(contentType));
        assertThat(context.getFileType(), is(fileType));
        assertThat(context.getEntities(), is(entityStream));
        assertThat(context.getEntityIds(), is(entityIdStream));
        assertThat(context.getFacets(), is(facets));
        assertThat(context.getFields(), is(fields));
        assertThat(context.getResource(), is(resource));
    }

    @Test
    void asCopyWorksAsExpected() {
        FileType fileType = FileType.FILE;
        MediaType contentType = MediaType.APPLICATION_JSON;
        Stream<String> entityStream = Stream.of("a", "b");
        Stream<String> entityIdStream = Stream.of("a.id", "b.id");
        List<Facet> facets = emptyList();
        MessageConverterContextFactory.Resource resource = MessageConverterContextFactory.Resource.UNIPROT;
        String fields = "field1,field2";

        MessageConverterContext<String> context = MessageConverterContext.<String>builder()
                .fileType(fileType)
                .contentType(contentType)
                .entities(entityStream)
                .entityIds(entityIdStream)
                .facets(facets)
                .fields(fields)
                .resource(resource)
                .build();

        MessageConverterContext<String> contextCopy = context.asCopy();

        assertThat(contextCopy.getContentType(), is(contentType));
        assertThat(contextCopy.getFileType(), is(fileType));
        assertThat(contextCopy.getEntities(), is(entityStream));
        assertThat(contextCopy.getEntityIds(), is(entityIdStream));
        assertThat(contextCopy.getFacets(), is(facets));
        assertThat(contextCopy.getFields(), is(fields));
        assertThat(contextCopy.getResource(), is(resource));
    }

    @Test
    void defaultFileTypeIsUsed() {
        MessageConverterContext<String> context = MessageConverterContext.<String>builder()
                .build();

        assertThat(context.getFileType(), is(DEFAULT_FILE_TYPE));
    }
}