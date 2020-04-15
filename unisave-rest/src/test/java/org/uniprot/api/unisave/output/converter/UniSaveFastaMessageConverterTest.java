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
class UniSaveFastaMessageConverterTest {
    private static UniSaveFastaMessageConverter converter;

    @BeforeAll
    static void setUp() {
        converter = new UniSaveFastaMessageConverter();
    }

    @Test
    void canConvertEntryToFasta() throws IOException {
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
                        ">Swissprot|P12345|Release 1111|DATE\nMASGAYSKYLFQIIGETVSSTNRGNKYNSFDHSRVDTRAGSFREAYNSKKKGSGRFGRKC\nFQIIGETVSSTNRG"
                                .getBytes());
    }
}
