package org.uniprot.api.uniprotkb.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.common.exception.ResourceNotFoundException;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UniProtKBEntryVersionServiceTest {

    @Mock
    private UniSaveClient uniSaveClient;

    @InjectMocks
    UniProtKBEntryVersionService uniProtKBEntryVersionService;

    private String SAMPLE_ENTRY_VERSION_HISTORY_RESPONSE =
            "{'results':[{'accession':'A0A1J4H6S2','database':'TrEMBL','entryVersion':9,'firstRelease':'2021_02/2021_02','firstReleaseDate':'07-Apr-2021','lastRelease':'2022_01/2022_01','lastReleaseDate':'23-Feb-2022','name':'A0A1J4H6S2_9STAP','sequenceVersion':1}]}";
    private String SAMPLE_ENTRY_VERSION_HISTORY_NOT_FOUND =
            "{'url':'http://rest.uniprot.org/unisave/B0B1J1H6A7','messages':['No entries for B0B1J1H6A7 were found']}";

    @Test
    void searchAccessionLastVersionFromUnisave() throws Exception {
        when(uniSaveClient.getUniSaveHistoryVersion("A0A1J4H6S2"))
                .thenReturn(SAMPLE_ENTRY_VERSION_HISTORY_RESPONSE);
        String entryVersion = uniProtKBEntryVersionService.getEntryVersion("last", "A0A1J4H6S2");
        assertEquals(entryVersion, "9");
    }

    @Test
    void uniSaveEntryHistoryEndpointWithAccessionNotPresent() throws Exception {
        when(uniSaveClient.getUniSaveHistoryVersion("B0B1J1H6A7"))
                .thenReturn(SAMPLE_ENTRY_VERSION_HISTORY_NOT_FOUND);
        Exception exception = assertThrows(ResourceNotFoundException.class, () -> {
            uniProtKBEntryVersionService.getEntryVersion("last", "B0B1J1H6A7");
        });
        String expectedMessage = "No entries for B0B1J1H6A7 were found";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }
}
