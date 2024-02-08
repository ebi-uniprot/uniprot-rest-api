package org.uniprot.api.uniprotkb.common.service.ec;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.core.cv.ec.ECEntry;
import org.uniprot.cv.ec.ECRepo;

@ExtendWith(MockitoExtension.class)
class ECServiceTest {
    private static final String EC = "ec";
    @Mock private ECEntry ecEntry;
    @Mock private ECRepo ecRepo;
    @InjectMocks private ECService ecService;

    @Test
    void getEC() {
        when(ecService.getEC(EC)).thenReturn(Optional.of(ecEntry));

        Optional<ECEntry> ec = ecService.getEC(EC);

        verify(ecRepo).getEC(EC);
        assertSame(ecEntry, ec.get());
    }

    @Test
    void getEC_empty() {
        when(ecService.getEC(EC)).thenReturn(Optional.empty());

        Optional<ECEntry> ec = ecService.getEC(EC);

        verify(ecRepo).getEC(EC);
        assertTrue(ec.isEmpty());
    }
}
