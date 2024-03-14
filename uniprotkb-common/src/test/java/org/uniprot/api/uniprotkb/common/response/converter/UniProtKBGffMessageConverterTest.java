package org.uniprot.api.uniprotkb.common.response.converter;

import static org.junit.jupiter.api.Assertions.*;
import static org.uniprot.api.uniprotkb.common.response.converter.UniProtKBGffMessageConverter.GFF_HEADER;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.core.json.parser.uniprot.UniProtKBEntryIT;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

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

        UniProtKBEntry entity = UniProtKBEntryIT.getCompleteColumnsUniProtEntry();
        uniProtKBEntryUniProtKBGffMessageConverter.before(messageContext, outputStream);
        uniProtKBEntryUniProtKBGffMessageConverter.writeEntity(entity, outputStream);

        String result = outputStream.toString(StandardCharsets.UTF_8);
        assertNotNull(result);
        String[] split = result.split("\n");
        assertEquals(GFF_HEADER, split[0].strip());
        assertTrue(split[1].contains("P00001"));
    }
}
