package org.uniprot.api.mapto.common.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.uniprot.api.mapto.common.model.MapToJob;
import org.uniprot.api.mapto.common.repository.MapToJobRepository;

@Service
public class MapToJobService {
    private final MapToJobRepository jobRepository;

    public MapToJobService(MapToJobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    public MapToJob createMapToJob(MapToJob mapToJob) {
        this.jobRepository
                .findById(mapToJob.getId())
                .ifPresent(
                        job -> {
                            throw new RuntimeException(
                                    "Mapto job exists with id + " + mapToJob.getId());
                        });
        return this.jobRepository.save(mapToJob);
    }

    public Optional<MapToJob> findMapToJob(String id) {
        return this.jobRepository.findById(id);
    }

    public void deleteMapToJob(String jobId) {
        this.jobRepository.deleteById(jobId);
    }

    public boolean mapToJobExists(String jobId) {
        return this.jobRepository.existsById(jobId);
    }
}
