package org.uniprot.api.idmapping.queue;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.uniprot.api.idmapping.model.EntryPair;

class IdMappingDownloadResultWriterFactoryTest {

    private static final UniParcIdMappingDownloadResultWriter uniparcWriter =
            Mockito.mock(UniParcIdMappingDownloadResultWriter.class);
    private static final UniProtKBIdMappingDownloadResultWriter uniprotWriter =
            Mockito.mock(UniProtKBIdMappingDownloadResultWriter.class);
    private static final UniRefIdMappingDownloadResultWriter unirefWriter =
            Mockito.mock(UniRefIdMappingDownloadResultWriter.class);

    @Test
    void canCreateUniProtKBWriter() {
        IdMappingDownloadResultWriterFactory factory =
                new IdMappingDownloadResultWriterFactory(
                        uniparcWriter, uniprotWriter, unirefWriter);
        AbstractIdMappingDownloadResultWriter<? extends EntryPair<?>, ?> result =
                factory.getResultWriter("uniprotkb");
        assertNotNull(result);
        assertEquals(uniprotWriter, result);
    }

    @Test
    void canCreateUniParcWriter() {
        IdMappingDownloadResultWriterFactory factory =
                new IdMappingDownloadResultWriterFactory(
                        uniparcWriter, uniprotWriter, unirefWriter);
        AbstractIdMappingDownloadResultWriter<? extends EntryPair<?>, ?> result =
                factory.getResultWriter("uniparc");
        assertNotNull(result);
        assertEquals(uniparcWriter, result);
    }

    @Test
    void canCreateUniRefWriterFrom50() {
        IdMappingDownloadResultWriterFactory factory =
                new IdMappingDownloadResultWriterFactory(
                        uniparcWriter, uniprotWriter, unirefWriter);
        AbstractIdMappingDownloadResultWriter<? extends EntryPair<?>, ?> result =
                factory.getResultWriter("uniref50");
        assertNotNull(result);
        assertEquals(unirefWriter, result);
    }

    @Test
    void canCreateUniRefWriterFrom90() {
        IdMappingDownloadResultWriterFactory factory =
                new IdMappingDownloadResultWriterFactory(
                        uniparcWriter, uniprotWriter, unirefWriter);
        AbstractIdMappingDownloadResultWriter<? extends EntryPair<?>, ?> result =
                factory.getResultWriter("uniref90");
        assertNotNull(result);
        assertEquals(unirefWriter, result);
    }

    @Test
    void canCreateUniRefWriterFrom100() {
        IdMappingDownloadResultWriterFactory factory =
                new IdMappingDownloadResultWriterFactory(
                        uniparcWriter, uniprotWriter, unirefWriter);
        AbstractIdMappingDownloadResultWriter<? extends EntryPair<?>, ?> result =
                factory.getResultWriter("uniref100");
        assertNotNull(result);
        assertEquals(unirefWriter, result);
    }
}
