package org.uniprot.api.uniparc.common.response.converter;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.uniprot.api.rest.output.converter.AbstractUUWHttpMessageConverterTest.writeAndAssertLifecycle;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.output.converter.AbstractEntityHttpMessageConverter;
import org.uniprot.api.rest.output.converter.AbstractXmlValidationTest;
import org.uniprot.api.uniparc.common.response.UniParcMessageConverterConfig;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.store.indexer.uniparc.mockers.UniParcEntryMocker;

class UniParcXmlMessageConverterTest extends AbstractXmlValidationTest<UniParcEntry> {

    @Override
    public String getXSDUrlLocation() {
        return "https://www.uniprot.org/docs/uniparc.xsd";
    }

    @Override
    public MessageConverterContext<UniParcEntry> getMessageConverterConfig() {
        return new UniParcMessageConverterConfig()
                .uniparcMessageConverterContextFactory()
                .get(MessageConverterContextFactory.Resource.UNIPARC, MediaType.APPLICATION_XML);
    }

    @Override
    public AbstractEntityHttpMessageConverter<UniParcEntry> getXmlConverter() {
        return new UniParcXmlMessageConverter("2021_04");
    }

    @Override
    protected UniParcEntry getEntry() {
        return UniParcEntryMocker.createUniParcEntry(2, "UPI");
    }

    @Override
    @Test
    @Disabled("Existing external schema validation remains disabled; lifecycle tests run locally")
    public void validateEntry() throws java.io.IOException {
        super.validateEntry();
    }

    @ParameterizedTest
    @MethodSource(
            "org.uniprot.api.rest.output.converter.AbstractUUWHttpMessageConverterTest#lifecycleCases")
    void UniParcFastaMessageConverterLifecycle(
            FileType fileType, boolean sourceFails, boolean outputFails) throws Exception {
        MessageConverterContext<UniParcEntry> context =
                MessageConverterContext.<UniParcEntry>builder()
                        .fileType(fileType)
                        .entityOnly(true)
                        .entities(Stream.of(getEntry()))
                        .build();
        byte[] result =
                writeAndAssertLifecycle(
                        new UniParcFastaMessageConverter(), context, sourceFails, outputFails);
        assertTrue(new String(result, StandardCharsets.UTF_8).startsWith(">"));
    }

    @ParameterizedTest
    @MethodSource(
            "org.uniprot.api.rest.output.converter.AbstractUUWHttpMessageConverterTest#lifecycleCases")
    void UniParcFastaExtendedMessageConverterLifecycle(
            FileType fileType, boolean sourceFails, boolean outputFails) throws Exception {
        MessageConverterContext<UniParcEntry> context =
                MessageConverterContext.<UniParcEntry>builder()
                        .fileType(fileType)
                        .entityOnly(true)
                        .entities(Stream.of(getEntry()))
                        .build();
        byte[] result =
                writeAndAssertLifecycle(
                        new UniParcFastaExtendedMessageConverter(),
                        context,
                        sourceFails,
                        outputFails);
        assertTrue(new String(result, StandardCharsets.UTF_8).startsWith(">"));
    }
}
