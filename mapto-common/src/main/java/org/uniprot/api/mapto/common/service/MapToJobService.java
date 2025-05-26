package org.uniprot.api.mapto.common.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.repository.search.ProblemPair;
import org.uniprot.api.mapto.common.model.MapToJob;
import org.uniprot.api.mapto.common.model.MapToJobRequest;
import org.uniprot.api.mapto.common.repository.MapToJobRepository;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.output.PredefinedAPIStatus;

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
        return this.jobRepository
                .findById(id)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Mapto job is not found with id " + id));
    }

    public void deleteMapToJob(String jobId) {
        this.jobRepository.deleteById(jobId);
    }

    public boolean mapToJobExists(String jobId) {
        return this.jobRepository.existsById(jobId);
    }

    public void updateStatus(String id, JobStatus jobStatus) {
        MapToJob mapToJob = findMapToJob(id);
        mapToJob.setStatus(jobStatus);
        mapToJob.setUpdated(LocalDateTime.now());
        jobRepository.save(mapToJob);
    }

    public void updateUpdated(String jobId) {
        MapToJob mapToJob = findMapToJob(jobId);
        mapToJob.setUpdated(LocalDateTime.now());
        jobRepository.save(mapToJob);
    }

    public void setTargetIds(String id, List<String> allMappedIds) {
        MapToJob mapToJob = findMapToJob(id);
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

    public void setErrors(String jobId, String error) {
        MapToJob mapToJob = findMapToJob(jobId);
        mapToJob.setStatus(JobStatus.ERROR);
        ProblemPair problemPair =
                new ProblemPair(PredefinedAPIStatus.LIMIT_EXCEED_ERROR.getCode(), error);
        mapToJob.setErrors(List.of(problemPair));
        jobRepository.save(mapToJob);
    }
}
