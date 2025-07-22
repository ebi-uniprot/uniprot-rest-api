package org.uniprot.api.uniprotkb.common.service.protlm;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.uniprotkb.common.repository.store.protlm.ProtLMStoreClient;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

@ExtendWith(MockitoExtension.class)
class ProtLMUniProtKBEntryServiceTest {

    @Mock private ProtLMStoreClient protLMStoreClient;

    @InjectMocks private ProtLMUniProtKBEntryService service;

    @Test
    void testGetProtLMEntry_success() {
        // Given
        String accession = "P12345";
        UniProtKBEntry mockEntry = mock(UniProtKBEntry.class);
        when(protLMStoreClient.getEntry(accession)).thenReturn(Optional.of(mockEntry));

        // When
        UniProtKBEntry result = service.getProtLMEntry(accession);

        // Then
        assertNotNull(result);
        assertEquals(mockEntry, result);
        verify(protLMStoreClient).getEntry(accession);
    }

    @Test
    void testGetProtLMEntry_notFound() {
        // Given
        String accession = "P00000";
        when(protLMStoreClient.getEntry(accession)).thenReturn(Optional.empty());

        // Then
        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> {
                            // When
                            service.getProtLMEntry(accession);
                        });

        assertEquals("No entry found for accession: P00000", exception.getMessage());
        verify(protLMStoreClient).getEntry(accession);
    }
}
