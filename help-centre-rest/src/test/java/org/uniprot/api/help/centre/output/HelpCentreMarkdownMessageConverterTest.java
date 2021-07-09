package org.uniprot.api.help.centre.output;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.uniprot.api.help.centre.model.HelpCentreEntry;

/**
 * @author lgonzales
 * @since 08/07/2021
 */
class HelpCentreMarkdownMessageConverterTest {

    @Test
    void canWriteEntity() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        HelpCentreEntry entity = HelpCentreEntry.builder().content("entity").build();

        HelpCentreMarkdownMessageConverter messageConverter =
                new HelpCentreMarkdownMessageConverter();
        messageConverter.writeEntity(entity, outputStream);

        String result = outputStream.toString(StandardCharsets.UTF_8);
        assertNotNull(result);
        assertEquals("entity", result);
    }
}
