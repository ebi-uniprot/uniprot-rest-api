package org.uniprot.api.help.centre.output;

import static org.junit.jupiter.api.Assertions.*;
import static org.uniprot.api.rest.output.converter.AbstractUUWHttpMessageConverterTest.writeAndAssertLifecycle;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.uniprot.api.help.centre.model.HelpCentreEntry;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.api.rest.output.context.MessageConverterContext;

/**
 * @author lgonzales
 * @since 08/07/2021
 */
class HelpCentreMarkdownMessageConverterTest {

    private static final String MARKDOWN_CONTENT =
            "---\n" + "title: title value\n" + "categories: cat1,cat2\n" + "---\n" + "entity";
    private static final String MARKDOWN_CONTENT_WITH_DATE =
            "---\n"
                    + "title: title value\n"
                    + "date: 1981-11-01\n"
                    + "categories: cat1,cat2\n"
                    + "---\n"
                    + "entity";

    @Test
    void canWriteEntity() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        HelpCentreEntry entity =
                HelpCentreEntry.builder()
                        .title("title value")
                        .categories(List.of("cat1", "cat2"))
                        .content("entity")
                        .build();

        HelpCentreMarkdownMessageConverter messageConverter =
                new HelpCentreMarkdownMessageConverter();
        messageConverter.writeEntity(entity, outputStream);

        String result = outputStream.toString(StandardCharsets.UTF_8);
        assertNotNull(result);
        assertEquals(MARKDOWN_CONTENT, result);
    }

    @Test
    void canWriteEntityWithDate() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        HelpCentreEntry entity =
                HelpCentreEntry.builder()
                        .title("title value")
                        .categories(List.of("cat1", "cat2"))
                        .releaseDate(LocalDate.of(1981, 11, 1))
                        .content("entity")
                        .build();

        HelpCentreMarkdownMessageConverter messageConverter =
                new HelpCentreMarkdownMessageConverter();
        messageConverter.writeEntity(entity, outputStream);

        String result = outputStream.toString(StandardCharsets.UTF_8);
        assertNotNull(result);
        assertEquals(MARKDOWN_CONTENT_WITH_DATE, result);
    }

    @ParameterizedTest
    @MethodSource(
            "org.uniprot.api.rest.output.converter.AbstractUUWHttpMessageConverterTest#lifecycleCases")
    void markdownLifecycle(FileType fileType, boolean sourceFails, boolean outputFails)
            throws Exception {
        HelpCentreEntry entry =
                HelpCentreEntry.builder()
                        .title("title value")
                        .categories(List.of("cat1", "cat2"))
                        .content("entity")
                        .build();
        MessageConverterContext<HelpCentreEntry> context =
                MessageConverterContext.<HelpCentreEntry>builder()
                        .fileType(fileType)
                        .entities(Stream.of(entry))
                        .build();
        byte[] result =
                writeAndAssertLifecycle(
                        new HelpCentreMarkdownMessageConverter(),
                        context,
                        sourceFails,
                        outputFails);
        assertEquals(MARKDOWN_CONTENT, new String(result, StandardCharsets.UTF_8));
    }
}
