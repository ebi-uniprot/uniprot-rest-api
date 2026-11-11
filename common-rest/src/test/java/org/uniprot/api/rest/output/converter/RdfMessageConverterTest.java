package org.uniprot.api.rest.output.converter;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.api.rest.output.context.MessageConverterContext;

/**
 * @author lgonzales
 * @since 2020-04-03
 */
class RdfMessageConverterTest {

    @Test
    void canWriteEntity() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        RdfMessageConverter rdfMessageConverter = new RdfMessageConverter();
        rdfMessageConverter.writeEntity("entity", outputStream);

        String result = outputStream.toString("UTF-8");

        assertNotNull(result);
        assertEquals("entity", result);
    }

    static Stream<Arguments> rdfLifecycleCases() {
        return AbstractUUWHttpMessageConverterTest.lifecycleCases()
                .flatMap(
                        args ->
                                Stream.of(
                                                Arguments.of(
                                                        new RdfMessageConverter(),
                                                        "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"/>"),
                                                Arguments.of(
                                                        new TurtleMessageConverter(),
                                                        "@prefix up: <http://purl.uniprot.org/core/> .\n<urn:entry> a up:Protein .\n"),
                                                Arguments.of(
                                                        new NTriplesMessageConverter(),
                                                        "<urn:entry> <urn:type> <urn:Protein> .\n"))
                                        .map(
                                                format ->
                                                        Arguments.of(
                                                                args.get()[0],
                                                                args.get()[1],
                                                                args.get()[2],
                                                                format.get()[0],
                                                                format.get()[1])));
    }

    @ParameterizedTest
    @MethodSource("rdfLifecycleCases")
    void closesSourceAndOutputForPlainAndGzip(
            FileType fileType,
            boolean sourceFails,
            boolean outputFails,
            AbstractEntityIdHttpMessageConverter<String> converter,
            String rdf)
            throws IOException {
        MessageConverterContext<String> context =
                MessageConverterContext.<String>builder()
                        .fileType(fileType)
                        .entityIds(Stream.of(rdf))
                        .build();
        byte[] result =
                AbstractUUWHttpMessageConverterTest.writeAndAssertLifecycle(
                        converter, context, sourceFails, outputFails);
        assertEquals(rdf, new String(result, StandardCharsets.UTF_8));
    }
}
