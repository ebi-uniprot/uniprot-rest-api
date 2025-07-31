package org.uniprot.api.uniprotkb.common.service.protnlm;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.uniprotkb.common.repository.store.protnlm.ProtNLMStoreClient;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

@ExtendWith(MockitoExtension.class)
class ProtNLMUniProtKBEntryServiceTest {

    @Mock private ProtNLMStoreClient protNLMStoreClient;

    @InjectMocks private ProtNLMUniProtKBEntryService service;

    @Test
    void testGetProtNLMEntry_success() {
        // Given
        String accession = "P12345";
        UniProtKBEntry mockEntry = mock(UniProtKBEntry.class);
        when(protNLMStoreClient.getEntry(accession)).thenReturn(Optional.of(mockEntry));

        // When
        UniProtKBEntry result = service.getProtNLMEntry(accession);

        // Then
        assertNotNull(result);
        assertEquals(mockEntry, result);
        verify(protNLMStoreClient).getEntry(accession);
    }

    @Test
    void testGetProtNLMEntry_notFound() {
        // Given
        String accession = "P00000";
        when(protNLMStoreClient.getEntry(accession)).thenReturn(Optional.empty());

        // Then
        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> {
                            // When
                            service.getProtNLMEntry(accession);
                        });

        assertEquals("No entry found for accession: P00000", exception.getMessage());
        verify(protNLMStoreClient).getEntry(accession);
    }
}
