package org.uniprot.api.async.download.messaging.result.idmapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.uniprot.api.common.repository.search.EntryPair;
import org.uniprot.store.config.idmapping.IdMappingFieldConfig;

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
                factory.getResultWriter(IdMappingFieldConfig.UNIPROTKB_STR);
        assertNotNull(result);
        assertEquals(uniprotWriter, result);
    }

    @Test
    void canCreateUniParcWriter() {
        IdMappingDownloadResultWriterFactory factory =
                new IdMappingDownloadResultWriterFactory(
                        uniparcWriter, uniprotWriter, unirefWriter);
        AbstractIdMappingDownloadResultWriter<? extends EntryPair<?>, ?> result =
                factory.getResultWriter(IdMappingFieldConfig.UNIPARC_STR);
        assertNotNull(result);
        assertEquals(uniparcWriter, result);
    }

    @Test
    void canCreateUniRefWriterFrom50() {
        IdMappingDownloadResultWriterFactory factory =
                new IdMappingDownloadResultWriterFactory(
                        uniparcWriter, uniprotWriter, unirefWriter);
        AbstractIdMappingDownloadResultWriter<? extends EntryPair<?>, ?> result =
                factory.getResultWriter(IdMappingFieldConfig.UNIREF_50_STR);
        assertNotNull(result);
        assertEquals(unirefWriter, result);
    }

    @Test
    void canCreateUniRefWriterFrom90() {
        IdMappingDownloadResultWriterFactory factory =
                new IdMappingDownloadResultWriterFactory(
                        uniparcWriter, uniprotWriter, unirefWriter);
        AbstractIdMappingDownloadResultWriter<? extends EntryPair<?>, ?> result =
                factory.getResultWriter(IdMappingFieldConfig.UNIREF_90_STR);
        assertNotNull(result);
        assertEquals(unirefWriter, result);
    }

    @Test
    void canCreateUniRefWriterFrom100() {
        IdMappingDownloadResultWriterFactory factory =
                new IdMappingDownloadResultWriterFactory(
                        uniparcWriter, uniprotWriter, unirefWriter);
        AbstractIdMappingDownloadResultWriter<? extends EntryPair<?>, ?> result =
                factory.getResultWriter(IdMappingFieldConfig.UNIREF_90_STR);
        assertNotNull(result);
        assertEquals(unirefWriter, result);
    }
}
