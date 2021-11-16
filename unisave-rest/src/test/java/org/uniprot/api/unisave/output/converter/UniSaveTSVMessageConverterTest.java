package org.uniprot.api.unisave.output.converter;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.uniprot.api.unisave.model.UniSaveEntry;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.uniprot.api.unisave.repository.domain.DatabaseEnum.SWISSPROT;

class UniSaveTSVMessageConverterTest {
    private static UniSaveTSVMessageConverter converter;
    private static OutputStream outputStream;

    @BeforeAll
    static void setUp() {
        outputStream = mock(OutputStream.class);
        converter = new UniSaveTSVMessageConverter();
    }

    @Test
    void canWriteCompleteRecord() throws IOException {
        UniSaveEntry entity1 =
                UniSaveEntry.builder()
                        .database(SWISSPROT.toString())
                        .accession("P12345")
                        .entryVersion(2)
                        .sequenceVersion(1)
                        .name("name")
                        .lastRelease("release")
                        .lastReleaseDate("date")
                        .replacingAcc(List.of("replacing"))
                        .mergedTo(List.of("merged"))
                        .build();

        converter.writeEntity(entity1, outputStream);

        ArgumentCaptor<byte[]> byteCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(outputStream).write(byteCaptor.capture());

        byte[] bytesWritten = byteCaptor.getValue();
        String writtenValue = new String(bytesWritten, 0, findEndOfString(bytesWritten));

        assertThat(writtenValue, is("2\t1\tname\tSwiss-Prot\trelease\tdate\treplacing\tmerged"));
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
