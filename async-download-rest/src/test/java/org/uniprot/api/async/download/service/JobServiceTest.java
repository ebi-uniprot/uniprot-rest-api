package org.uniprot.api.async.download.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.uniprot.api.async.download.messaging.repository.DownloadJobRepository;
import org.uniprot.api.async.download.model.job.DownloadJob;

public abstract class JobServiceTest<T extends DownloadJob> {
    public static final String ID = "id";
    protected T downloadJob;
    protected Optional<T> downloadJobOpt;
    protected DownloadJobRepository<T> downloadJobRepository;
    protected JobService<T> jobService;

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
        when(downloadJobRepository.findById(ID)).thenReturn(downloadJobOpt);

        assertSame(downloadJobOpt, jobService.find(ID));
    }

    @Test
    void exist() {
        Boolean answer = false;
        when(downloadJobRepository.existsById(ID)).thenReturn(answer);

        assertSame(answer, jobService.exist(ID));
    }
}
