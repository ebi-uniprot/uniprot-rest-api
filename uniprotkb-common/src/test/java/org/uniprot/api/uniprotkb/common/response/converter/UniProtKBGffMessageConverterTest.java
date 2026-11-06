package org.uniprot.api.uniprotkb.common.response.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.uniprot.api.rest.output.converter.AbstractUUWHttpMessageConverterTest.writeAndAssertLifecycle;
import static org.uniprot.api.uniprotkb.common.response.converter.UniProtKBGffMessageConverter.GFF_HEADER;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.core.json.parser.uniprot.UniProtKBEntryIT;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.indexer.uniprot.mockers.UniProtEntryMocker;

class UniProtKBGffMessageConverterTest {
    private final UniProtKBGffMessageConverter uniProtKBEntryUniProtKBGffMessageConverter =
            new UniProtKBGffMessageConverter();

    @Test
    void before() throws Exception {
        MessageConverterContext<UniProtKBEntry> messageContext =
                MessageConverterContext.<UniProtKBEntry>builder()
                        .fields("accession,organism_name,gene_orf")
                        .build();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        uniProtKBEntryUniProtKBGffMessageConverter.before(messageContext, outputStream);

        String result = outputStream.toString(StandardCharsets.UTF_8);
        assertNotNull(result);
        assertEquals(GFF_HEADER, result.strip());
    }

    @Test
    void writeEntity() throws Exception {
        MessageConverterContext<UniProtKBEntry> messageContext =
                MessageConverterContext.<UniProtKBEntry>builder()
                        .fields("accession,ft_site,cc_function,cc_similarity")
                        .build();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        List<UniProtKBEntry> entities =
                List.of(
                        UniProtKBEntryIT.getCompleteColumnsUniProtEntry(),
                        UniProtKBEntryIT.getCompleteColumnsUniProtEntry());

        uniProtKBEntryUniProtKBGffMessageConverter.before(messageContext, outputStream);
        for (UniProtKBEntry entity : entities) {
            uniProtKBEntryUniProtKBGffMessageConverter.writeEntity(entity, outputStream);
        }

        String result = outputStream.toString(StandardCharsets.UTF_8);
        assertNotNull(result);
        assertEquals(1, StringUtils.countMatches(result, GFF_HEADER));
        assertEquals(0, StringUtils.countMatches(result, "sequence-region"));
        String[] split = result.split("\n");
        assertEquals(GFF_HEADER, split[0].strip());
        assertEquals(
                "P00001\tUniProtKB\tInitiator methionine\t2\t8\t.\t.\t.\tNote=Description value 123;Ontology_term=ECO:0000269;evidence=ECO:0000269|PubMed:11389730;Dbxref=PMID:11389730\t",
                split[1]);
    }

    @Test
    void writeEntity_forEmptyList() throws Exception {
        MessageConverterContext<UniProtKBEntry> messageContext =
                MessageConverterContext.<UniProtKBEntry>builder()
                        .fields("accession,ft_site,cc_function,cc_similarity")
                        .build();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        List<UniProtKBEntry> entities = List.of();

        uniProtKBEntryUniProtKBGffMessageConverter.before(messageContext, outputStream);
        for (UniProtKBEntry entity : entities) {
            uniProtKBEntryUniProtKBGffMessageConverter.writeEntity(entity, outputStream);
        }

        String result = outputStream.toString(StandardCharsets.UTF_8);
        assertNotNull(result);
        String[] split = result.split("\n");
        assertEquals(1, split.length);
        assertEquals(GFF_HEADER, split[0].strip());
    }

    @ParameterizedTest
    @MethodSource(
            "org.uniprot.api.rest.output.converter.AbstractUUWHttpMessageConverterTest#lifecycleCases")
    void gffLifecycle(FileType fileType, boolean sourceFails, boolean outputFails)
            throws Exception {
        MessageConverterContext<UniProtKBEntry> context =
                MessageConverterContext.<UniProtKBEntry>builder()
                        .fileType(fileType)
                        .entities(Stream.of(UniProtKBEntryIT.getCompleteColumnsUniProtEntry()))
                        .build();
        byte[] result =
                writeAndAssertLifecycle(
                        new UniProtKBGffMessageConverter(), context, sourceFails, outputFails);
        assertTrue(new String(result, StandardCharsets.UTF_8).contains(GFF_HEADER));
    }

    @ParameterizedTest
    @MethodSource(
            "org.uniprot.api.rest.output.converter.AbstractUUWHttpMessageConverterTest#lifecycleCases")
    void fastaLifecycle(FileType fileType, boolean sourceFails, boolean outputFails)
            throws Exception {
        MessageConverterContext<UniProtKBEntry> context =
                MessageConverterContext.<UniProtKBEntry>builder()
                        .fileType(fileType)
                        .entities(Stream.of(UniProtKBEntryIT.getCompleteColumnsUniProtEntry()))
                        .build();
        byte[] result =
                writeAndAssertLifecycle(
                        new UniProtKBFastaMessageConverter(), context, sourceFails, outputFails);
        assertTrue(new String(result, StandardCharsets.UTF_8).contains(">sp|P00001|"));
    }

    @ParameterizedTest
    @MethodSource(
            "org.uniprot.api.rest.output.converter.AbstractUUWHttpMessageConverterTest#lifecycleCases")
    void flatFileLifecycle(FileType fileType, boolean sourceFails, boolean outputFails)
            throws Exception {
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        MessageConverterContext<UniProtKBEntry> context =
                MessageConverterContext.<UniProtKBEntry>builder()
                        .fileType(fileType)
                        .entities(Stream.of(entry))
                        .build();
        byte[] result =
                writeAndAssertLifecycle(
                        new UniProtKBFlatFileMessageConverter(), context, sourceFails, outputFails);
        assertTrue(
                new String(result, StandardCharsets.UTF_8)
                        .contains("AC   " + entry.getPrimaryAccession().getValue() + ";"));
    }
}
