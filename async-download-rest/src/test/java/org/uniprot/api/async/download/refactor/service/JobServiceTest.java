package org.uniprot.api.async.download.refactor.service;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.messaging.repository.DownloadJobRepository;
import org.uniprot.api.async.download.model.common.DownloadJob;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {
    public static final String ID = "id";
    @Mock private DownloadJob downloadJob;
    @Mock private DownloadJobRepository downloadJobRepository;
    @InjectMocks private JobService jobService;

    @Test
    void create() {
        jobService.save(downloadJob);

        verify(downloadJobRepository).save(downloadJob);
    }

    @Test
    void update() {
        Map<String, Object> fieldsToUpdate = Map.of();

        jobService.update(ID, fieldsToUpdate);

        verify(downloadJobRepository).update(ID, fieldsToUpdate);
    }

    @Test
    void delete() {
        jobService.delete(ID);

        verify(downloadJobRepository).deleteById(ID);
    }

    @Test
    void find() {
        Optional<DownloadJob> expected = Mockito.mock(Optional.class);
        when(downloadJobRepository.findById(ID)).thenReturn(expected);

        assertSame(expected, jobService.find(ID));
    }

    @Test
    void exist() {
        Boolean answer = false;
        when(downloadJobRepository.existsById(ID)).thenReturn(answer);

        assertSame(answer, jobService.exist(ID));
    }
}
