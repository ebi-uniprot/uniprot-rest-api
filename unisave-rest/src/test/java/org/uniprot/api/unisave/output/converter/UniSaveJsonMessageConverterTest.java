package org.uniprot.api.unisave.output.converter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.uniprot.api.unisave.repository.domain.DatabaseEnum.SWISSPROT;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.unisave.UniSaveEntityMocker;
import org.uniprot.api.unisave.model.UniSaveEntry;
import org.uniprot.api.unisave.repository.domain.impl.EntryImpl;

/**
 * Created 15/04/20
 *
 * @author Edd
 */
class UniSaveJsonMessageConverterTest {

    private static UniSaveJsonMessageConverter converter;
    private static OutputStream outputStream;

    @BeforeAll
    static void setUp() throws IOException {
        outputStream = mock(OutputStream.class);
        converter = new UniSaveJsonMessageConverter();
    }

    @Test
    void canWriteJson() throws IOException {
        EntryImpl mockEntry = UniSaveEntityMocker.mockEntry("P12345", 1);
        UniSaveEntry entry =
                UniSaveEntry.builder()
                        .database(SWISSPROT.toString())
                        .accession(mockEntry.getAccession())
                        .content(mockEntry.getEntryContent().getFullContent())
                        .firstRelease("1111")
                        .firstReleaseDate("DATE")
                        .build();
        MessageConverterContext<UniSaveEntry> messageConverterContext =
                MessageConverterContext.<UniSaveEntry>builder()
                        .entityOnly(true)
                        .entities(Stream.of(entry))
                        .build();

        converter.writeContents(
                messageConverterContext, outputStream, Instant.now(), new AtomicInteger(0));

        ArgumentCaptor<byte[]> byteCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(outputStream)
                .write(byteCaptor.capture(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt());

        byte[] bytesWritten = byteCaptor.getValue();
        String writtenValue = new String(bytesWritten, 0, findEndOfString(bytesWritten));
        assertThat(
                writtenValue,
                is(
                        "{\"accession\":\"P12345\",\"database\":\"Swiss-Prot\",\"firstRelease\":\"1111\",\"firstReleaseDate\":\"DATE\",\"content\":\"ID   P12345_ID        Unreviewed;        60 AA.\\nAC   P12345;\\nDT   13-FEB-2019, integrated into UniProtKB/TrEMBL.\\nDT   13-FEB-2019, sequence version 1.\\nDT   11-DEC-2019, entry version 1.\\nDE   SubName: Full=Uncharacterized protein {ECO:0000313|EMBL:AYX10384.1};\\nGN   ORFNames=EGX52_05955 {ECO:0000313|EMBL:AYX10384.1};\\nOS   Yersinia pseudotuberculosis.\\nOC   Bacteria; Proteobacteria; Gammaproteobacteria; Enterobacterales;\\nOC   Yersiniaceae; Yersinia.\\nOX   NCBI_TaxID=633 {ECO:0000313|EMBL:AYX10384.1, ECO:0000313|Proteomes:UP000277634};\\nRN   [1] {ECO:0000313|Proteomes:UP000277634}\\nRP   NUCLEOTIDE SEQUENCE [LARGE SCALE GENOMIC DNA].\\nRC   STRAIN=FDAARGOS_580 {ECO:0000313|Proteomes:UP000277634};\\nRA   Goldberg B., Campos J., Tallon L., Sadzewicz L., Zhao X., Vavikolanu K.,\\nRA   Mehta A., Aluvathingal J., Nadendla S., Geyer C., Nandy P., Yan Y.,\\nRA   Sichtig H.;\\nRT   \\\"FDA dAtabase for Regulatory Grade micrObial Sequences (FDA-ARGOS):\\nRT   Supporting development and validation of Infectious Disease Dx tests.\\\";\\nRL   Submitted (NOV-2018) to the EMBL/GenBank/DDBJ databases.\\nDR   EMBL; CP033715; AYX10384.1; -; Genomic_DNA.\\nDR   RefSeq; WP_072092108.1; NZ_PDEJ01000002.1.\\nDR   Proteomes; UP000277634; Chromosome.\\nPE   4: Predicted;\\nSQ   SEQUENCE   60 AA;  6718 MW;  701D8D73381524E8 CRC64;\\n     MASGAYSKYL FQIIGETVSS TNRGNKYNSF DHSRVDTRAG SFREAYNSKK KGSGRFGRKC\\n     FQIIGETVSS TNRG\\n//\\n\"}"));
    }

    private int findEndOfString(byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] == 0) {
                return i;
            }
        }
        return bytes.length - 1;
    }
}
