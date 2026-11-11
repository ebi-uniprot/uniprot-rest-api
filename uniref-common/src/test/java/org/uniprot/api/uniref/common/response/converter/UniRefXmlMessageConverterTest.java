package org.uniprot.api.uniref.common.response.converter;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.uniprot.api.rest.output.converter.AbstractUUWHttpMessageConverterTest.writeAndAssertLifecycle;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.output.converter.AbstractEntityHttpMessageConverter;
import org.uniprot.api.rest.output.converter.AbstractXmlValidationTest;
import org.uniprot.api.uniref.common.response.UniRefMessageConverterConfig;
import org.uniprot.core.uniref.UniRefEntry;
import org.uniprot.core.uniref.UniRefType;
import org.uniprot.store.indexer.uniref.mockers.UniRefEntryMocker;

class UniRefXmlMessageConverterTest extends AbstractXmlValidationTest<UniRefEntry> {

    @Override
    public String getXSDUrlLocation() {
        return "https://www.uniprot.org/docs/uniref.xsd";
    }

    @Override
    public MessageConverterContext<UniRefEntry> getMessageConverterConfig() {
        return new UniRefMessageConverterConfig()
                .uniRefMessageConverterContextFactory()
                .get(MessageConverterContextFactory.Resource.UNIREF, MediaType.APPLICATION_XML);
    }

    @Override
    public AbstractEntityHttpMessageConverter<UniRefEntry> getXmlConverter() {
        return new UniRefXmlMessageConverter("2021_04", "2021-09-29");
    }

    @Override
    protected UniRefEntry getEntry() {
        return UniRefEntryMocker.createEntry(1, UniRefType.UniRef100);
    }

    @ParameterizedTest
    @MethodSource(
            "org.uniprot.api.rest.output.converter.AbstractUUWHttpMessageConverterTest#lifecycleCases")
    void UniRefFastaMessageConverterLifecycle(
            FileType fileType, boolean sourceFails, boolean outputFails) throws Exception {
        MessageConverterContext<UniRefEntry> context =
                MessageConverterContext.<UniRefEntry>builder()
                        .fileType(fileType)
                        .entityOnly(true)
                        .entities(Stream.of(getEntry()))
                        .build();
        byte[] result =
                writeAndAssertLifecycle(
                        new UniRefFastaMessageConverter(), context, sourceFails, outputFails);
        assertTrue(new String(result, StandardCharsets.UTF_8).startsWith(">"));
    }
}
