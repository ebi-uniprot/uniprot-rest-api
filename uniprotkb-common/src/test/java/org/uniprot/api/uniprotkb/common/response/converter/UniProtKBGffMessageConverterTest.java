package org.uniprot.api.uniprotkb.common.response.converter;

import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Test;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.core.json.parser.uniprot.UniProtKBEntryIT;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.uniprot.api.uniprotkb.common.response.converter.UniProtKBGffMessageConverter.GFF_HEADER;

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
        List<UniProtKBEntry> entities = List.of(UniProtKBEntryIT.getCompleteColumnsUniProtEntry(), UniProtKBEntryIT.getCompleteColumnsUniProtEntry());

        uniProtKBEntryUniProtKBGffMessageConverter.before(messageContext, outputStream);
        for (UniProtKBEntry entity : entities) {
            uniProtKBEntryUniProtKBGffMessageConverter.writeEntity(entity, outputStream);
        }

        String result = outputStream.toString(StandardCharsets.UTF_8);
        assertNotNull(result);
        assertEquals(1, StringUtils.countMatches(result, GFF_HEADER));
        String[] split = result.split("\n");
        assertEquals(GFF_HEADER, split[0].strip());
        assertEquals("P00001\tUniProtKB\tInitiator methionine\t2\t8\t.\t.\t.\tNote=Description value 123;Ontology_term=ECO:0000269;evidence=ECO:0000269|PubMed:11389730;Dbxref=PMID:11389730\t", split[1]);
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
        assertEquals(GFF_HEADER, split[0].strip());
        assertEquals(1, split.length);
    }
}
