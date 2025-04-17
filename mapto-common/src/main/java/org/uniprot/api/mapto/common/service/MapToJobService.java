package org.uniprot.api.mapto.common.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.uniprot.api.mapto.common.model.MapToJob;
import org.uniprot.api.mapto.common.model.MapToJobRequest;
import org.uniprot.api.mapto.common.repository.MapToJobRepository;
import org.uniprot.api.rest.download.model.JobStatus;

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
                            throw new IllegalArgumentException(
                                    "Mapto job exists with id + " + mapToJob.getId());
                        });
        return this.jobRepository.save(mapToJob);
    }

    public MapToJob findMapToJob(String id) {
        return this.jobRepository.findById(id).orElseThrow(
                () ->
                        new IllegalArgumentException(
                                "Mapto job does not exist with id + " + id));
    }

    public void deleteMapToJob(String jobId) {
        this.jobRepository.deleteById(jobId);
    }

    public boolean mapToJobExists(String jobId) {
        return this.jobRepository.existsById(jobId);
    }

    public void updateStatus(String id, JobStatus jobStatus) {
        MapToJob mapToJob =
                findMapToJob(id);
        mapToJob.setStatus(jobStatus);
        mapToJob.setUpdated(LocalDateTime.now());
        jobRepository.save(mapToJob);
    }

    public void setTargetIds(String id, List<String> allMappedIds) {
        MapToJob mapToJob =
                findMapToJob(id);
        mapToJob.setTargetIds(allMappedIds);
        mapToJob.setUpdated(LocalDateTime.now());
        mapToJob.setStatus(JobStatus.FINISHED);
        jobRepository.save(mapToJob);
    }

    public MapToJob createMapToJob(String jobId, MapToJobRequest mapToJobRequest) {
        LocalDateTime now = LocalDateTime.now();
        MapToJob mapToJob = new MapToJob();
        mapToJob.setId(jobId);
        mapToJob.setSourceDB(mapToJobRequest.getSource());
        mapToJob.setTargetDB(mapToJobRequest.getTarget());
        mapToJob.setQuery(mapToJobRequest.getQuery());
        mapToJob.setCreated(now);
        mapToJob.setExtraParams(mapToJobRequest.getExtraParams());
        mapToJob.setUpdated(now);
        mapToJob.setStatus(JobStatus.NEW);
        return createMapToJob(mapToJob);
    }
}
