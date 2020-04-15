package org.uniprot.api.unisave.output.converter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.uniprot.api.unisave.repository.domain.DatabaseEnum.Swissprot;

import java.io.IOException;
import java.io.OutputStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.uniprot.api.unisave.UniSaveEntryMocker;
import org.uniprot.api.unisave.model.UniSaveEntry;
import org.uniprot.api.unisave.repository.domain.impl.EntryImpl;

/**
 * Created 15/04/20
 *
 * @author Edd
 */
class UniSaveFlatFileMessageConverterTest {

    private static UniSaveFlatFileMessageConverter converter;

    @BeforeAll
    static void setUp() {
        converter = new UniSaveFlatFileMessageConverter();
    }

    @Test
    void canWriteFlatFile() throws IOException {
        EntryImpl mockEntry = UniSaveEntryMocker.mockEntry("P12345", 1);
        UniSaveEntry entry =
                UniSaveEntry.builder()
                        .database(Swissprot.name())
                        .accession(mockEntry.getAccession())
                        .content(mockEntry.getEntryContent().getFullcontent())
                        .firstRelease("1111")
                        .firstReleaseDate("DATE")
                        .build();

        OutputStream outputStream = mock(OutputStream.class);
        converter.writeEntity(entry, outputStream);
        verify(outputStream)
                .write(
                        ("ID   P12345_ID        Unreviewed;        60 AA.\n"
                                        + "AC   P12345;\n"
                                        + "DT   13-FEB-2019, integrated into UniProtKB/TrEMBL.\n"
                                        + "DT   13-FEB-2019, sequence version 1.\n"
                                        + "DT   11-DEC-2019, entry version 1.\n"
                                        + "DE   SubName: Full=Uncharacterized protein {ECO:0000313|EMBL:AYX10384.1};\n"
                                        + "GN   ORFNames=EGX52_05955 {ECO:0000313|EMBL:AYX10384.1};\n"
                                        + "OS   Yersinia pseudotuberculosis.\n"
                                        + "OC   Bacteria; Proteobacteria; Gammaproteobacteria; Enterobacterales;\n"
                                        + "OC   Yersiniaceae; Yersinia.\n"
                                        + "OX   NCBI_TaxID=633 {ECO:0000313|EMBL:AYX10384.1, ECO:0000313|Proteomes:UP000277634};\n"
                                        + "RN   [1] {ECO:0000313|Proteomes:UP000277634}\n"
                                        + "RP   NUCLEOTIDE SEQUENCE [LARGE SCALE GENOMIC DNA].\n"
                                        + "RC   STRAIN=FDAARGOS_580 {ECO:0000313|Proteomes:UP000277634};\n"
                                        + "RA   Goldberg B., Campos J., Tallon L., Sadzewicz L., Zhao X., Vavikolanu K.,\n"
                                        + "RA   Mehta A., Aluvathingal J., Nadendla S., Geyer C., Nandy P., Yan Y.,\n"
                                        + "RA   Sichtig H.;\n"
                                        + "RT   \"FDA dAtabase for Regulatory Grade micrObial Sequences (FDA-ARGOS):\n"
                                        + "RT   Supporting development and validation of Infectious Disease Dx tests.\";\n"
                                        + "RL   Submitted (NOV-2018) to the EMBL/GenBank/DDBJ databases.\n"
                                        + "DR   EMBL; CP033715; AYX10384.1; -; Genomic_DNA.\n"
                                        + "DR   RefSeq; WP_072092108.1; NZ_PDEJ01000002.1.\n"
                                        + "DR   Proteomes; UP000277634; Chromosome.\n"
                                        + "PE   4: Predicted;\n"
                                        + "SQ   SEQUENCE   60 AA;  6718 MW;  701D8D73381524E8 CRC64;\n"
                                        + "     MASGAYSKYL FQIIGETVSS TNRGNKYNSF DHSRVDTRAG SFREAYNSKK KGSGRFGRKC\n"
                                        + "     FQIIGETVSS TNRG\n"
                                        + "//\n")
                                .getBytes());
    }
}
