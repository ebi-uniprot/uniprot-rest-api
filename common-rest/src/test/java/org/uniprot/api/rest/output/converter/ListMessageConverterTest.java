package org.uniprot.api.rest.output.converter;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.api.rest.output.context.MessageConverterContext;

/**
 * @author lgonzales
 * @since 2020-04-03
 */
class ListMessageConverterTest {

    @Test
    void canWriteEntity() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        ListMessageConverter messageConverter = new ListMessageConverter();
        messageConverter.writeEntity("entity", outputStream);

        String result = outputStream.toString("UTF-8");
        assertNotNull(result);
        assertEquals("entity\n", result);
    }

    @ParameterizedTest
    @MethodSource(
            "org.uniprot.api.rest.output.converter.AbstractUUWHttpMessageConverterTest#lifecycleCases")
    void closesSourceAndOutputForPlainAndGzip(
            FileType fileType, boolean sourceFails, boolean outputFails) throws IOException {
        MessageConverterContext<String> context =
                MessageConverterContext.<String>builder()
                        .fileType(fileType)
                        .entityIds(Stream.of("entity"))
                        .build();
        byte[] result =
                AbstractUUWHttpMessageConverterTest.writeAndAssertLifecycle(
                        new ListMessageConverter(), context, sourceFails, outputFails);
        assertEquals("entity\n", new String(result, StandardCharsets.UTF_8));
    }
}
