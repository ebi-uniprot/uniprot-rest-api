package org.uniprot.api.async.download.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.uniprot.api.async.download.messaging.repository.DownloadJobRepository;
import org.uniprot.api.async.download.model.job.DownloadJob;

public abstract class JobServiceTest<R extends DownloadJob> {
    public static final String ID = "id";
    protected R downloadJob;
    protected Optional<R> downloadJobOpt;
    protected DownloadJobRepository<R> downloadJobRepository;
    protected JobService<R> jobService;

    @Test
    void create() {
        jobService.create(downloadJob);

        verify(downloadJobRepository).save(downloadJob);
    }

    @Test
    void create_whenAlreadyPresent() {
        when(downloadJob.getId()).thenReturn(ID);
        when(downloadJobRepository.findById(ID)).thenReturn(downloadJobOpt);

        assertThrows(IllegalStateException.class, () -> jobService.create(downloadJob));
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
