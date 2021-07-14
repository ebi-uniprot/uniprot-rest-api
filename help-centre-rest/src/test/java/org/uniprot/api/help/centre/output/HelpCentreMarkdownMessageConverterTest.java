package org.uniprot.api.help.centre.output;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.uniprot.api.help.centre.model.HelpCentreEntry;

/**
 * @author lgonzales
 * @since 08/07/2021
 */
class HelpCentreMarkdownMessageConverterTest {

    public static final String MARKDOWN_CONTENT =
            "---\n" + "title: title value\n" + "categories: cat1,cat2\n" + "---\n" + "entity";

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
}
