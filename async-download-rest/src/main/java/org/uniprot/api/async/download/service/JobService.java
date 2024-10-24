package org.uniprot.api.async.download.service;

import java.util.Map;
import java.util.Optional;

import org.uniprot.api.async.download.messaging.repository.DownloadJobRepository;
import org.uniprot.api.async.download.model.job.DownloadJob;

public class JobService<R extends DownloadJob> {
    private final DownloadJobRepository<R> downloadJobRepository;

    public JobService(DownloadJobRepository<R> downloadJobRepository) {
        this.downloadJobRepository = downloadJobRepository;
    }

    public R save(R downloadJob) {
        return downloadJobRepository.save(downloadJob);
    }

    public void update(String id, Map<String, Object> fieldsToUpdate) {
        downloadJobRepository.update(id, fieldsToUpdate);
    }

    public void delete(String id) {
        downloadJobRepository.deleteById(id);
    }

    public Optional<R> find(String id) {
        return downloadJobRepository.findById(id);
    }

    public boolean exist(String id) {
        return downloadJobRepository.existsById(id);
    }
}
