package org.uniprot.api.unisave.output.converter;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.uniprot.api.unisave.UniSaveEntityMocker;
import org.uniprot.api.unisave.model.UniSaveEntry;
import org.uniprot.api.unisave.repository.domain.impl.EntryImpl;

import java.io.IOException;
import java.io.OutputStream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.uniprot.api.unisave.repository.domain.DatabaseEnum.Swissprot;

/**
 * Created 15/04/20
 *
 * @author Edd
 */
class UniSaveFastaMessageConverterTest {
    private static UniSaveFastaMessageConverter converter;

    @BeforeAll
    static void setUp() {
        converter = new UniSaveFastaMessageConverter();
    }

    @Test
    void canConvertOldEntryToSimpleFasta() throws IOException {
        EntryImpl mockEntry = UniSaveEntityMocker.mockEntry("P12345", 1);
        UniSaveEntry entry =
                UniSaveEntry.builder()
                        .database(Swissprot.name())
                        .accession(mockEntry.getAccession())
                        .content(mockEntry.getEntryContent().getFullContent())
                        .firstRelease("1111")
                        .firstReleaseDate("DATE")
                        .isCurrentRelease(false)
                        .build();
        OutputStream outputStream = mock(OutputStream.class);
        converter.writeEntity(entry, outputStream);
        verify(outputStream)
                .write(
                        (">tr|P12345|Release 1111|DATE\n"
                                        + "MASGAYSKYLFQIIGETVSSTNRGNKYNSFDHSRVDTRAGSFREAYNSKKKGSGRFGRKC\n"
                                        + "FQIIGETVSSTNRG\n")
                                .getBytes());
    }

    @Test
    void canConvertCurrentReleaseEntryToFullFasta() throws IOException {
        EntryImpl mockEntry = UniSaveEntityMocker.mockEntry("P12345", 1);
        UniSaveEntry entry =
                UniSaveEntry.builder()
                        .database(Swissprot.name())
                        .accession(mockEntry.getAccession())
                        .content(mockEntry.getEntryContent().getFullContent())
                        .firstRelease("1111")
                        .firstReleaseDate("DATE")
                        .isCurrentRelease(true)
                        .build();
        OutputStream outputStream = mock(OutputStream.class);
        converter.writeEntity(entry, outputStream);
        verify(outputStream)
                .write(
                        (">tr|P12345|P12345_ID Uncharacterized protein OS=Yersinia pseudotuberculosis OX=633 GN=EGX52_05955 PE=4 SV=1\n"
                                        + "MASGAYSKYLFQIIGETVSSTNRGNKYNSFDHSRVDTRAGSFREAYNSKKKGSGRFGRKC\n"
                                        + "FQIIGETVSSTNRG\n")
                                .getBytes());
    }
}
