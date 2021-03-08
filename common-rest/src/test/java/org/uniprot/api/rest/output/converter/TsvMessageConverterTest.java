package org.uniprot.api.rest.output.converter;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.core.json.parser.uniprot.UniProtKBEntryIT;
import org.uniprot.core.parser.tsv.uniprot.UniProtKBEntryValueMapper;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;

/**
 * @author lgonzales
 * @since 2020-04-03
 */
class TsvMessageConverterTest {

    private static TsvMessageConverter<UniProtKBEntry> tsvMessageConverter;

    @BeforeAll
    static void init() {
        ReturnFieldConfig returnFieldConfig =
                ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.UNIPROTKB);
        UniProtKBEntryValueMapper mapper = new UniProtKBEntryValueMapper();
        tsvMessageConverter =
                new TsvMessageConverter<UniProtKBEntry>(
                        UniProtKBEntry.class, returnFieldConfig, mapper);
    }

    @Test
    void canWriteHeader() throws IOException {
        MessageConverterContext<UniProtKBEntry> messageContext =
                MessageConverterContext.<UniProtKBEntry>builder()
                        .fields("accession,organism_name,gene_orf")
                        .build();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        tsvMessageConverter.before(messageContext, outputStream);
        tsvMessageConverter.cleanUp();

        String result = outputStream.toString("UTF-8");
        assertNotNull(result);
        assertEquals("Entry\tOrganism\tGene Names (ORF)\n", result);
    }

    @Test
    void canWriteBody() throws IOException {
        MessageConverterContext<UniProtKBEntry> messageContext =
                MessageConverterContext.<UniProtKBEntry>builder()
                        .fields("accession,ft_site,cc_function")
                        .build();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        UniProtKBEntry entity = UniProtKBEntryIT.getCompleteColumnsUniProtEntry();
        tsvMessageConverter.before(messageContext, outputStream);
        tsvMessageConverter.writeEntity(entity, outputStream);
        tsvMessageConverter.cleanUp();

        String result = outputStream.toString("UTF-8");
        assertNotNull(result);
        assertTrue(
                result.contains(
                        "P00001\t"
                                + "SITE sequence 1:2..8 /note=\"description value 123\" "
                                + "/evidence=\"ECO:0000269|PubMed:11389730\"\t"
                                + "FUNCTION: [Isoform 4]: value. {ECO:0000256|PIRNR:PIRNR001360}."));
    }
}
