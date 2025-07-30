package org.uniprot.api.mapto.common.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.repository.search.ProblemPair;
import org.uniprot.api.mapto.common.model.MapToJob;
import org.uniprot.api.mapto.common.model.MapToJobRequest;
import org.uniprot.api.mapto.common.model.MapToResult;
import org.uniprot.api.mapto.common.repository.MapToJobRepository;
import org.uniprot.api.rest.download.model.JobStatus;

@Service
public class MapToJobService {
    public static final String INCLUDE_ISOFORM = "includeIsoform";
    private final MapToJobRepository jobRepository;
    private final JdbcTemplate jdbcTemplate;
    private final int batchSize;

    public MapToJobService(MapToJobRepository jobRepository, JdbcTemplate jdbcTemplate, @Value("${mapto.jdbc.update.batch_size}") int batchSize) {
        this.jobRepository = jobRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.batchSize = batchSize;
    }

    public MapToJob createMapToJob(MapToJob mapToJob) {
        this.jobRepository
                .findByJobId(mapToJob.getJobId())
                .ifPresent(
                        job -> {
                            throw new IllegalArgumentException(
                                    "Mapto job exists with id + " + mapToJob.getJobId());
                        });
        return this.jobRepository.save(mapToJob);
    }

    public MapToJob findMapToJob(String id) {
        return this.jobRepository
                .findByJobId(id)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Mapto job is not found with id " + id));
    }

    public void deleteMapToJob(String jobId) {
        this.jobRepository.deleteByJobId(jobId);
    }

    public boolean mapToJobExists(String jobId) {
        return this.jobRepository.existsByJobId(jobId);
    }

    public void updateStatus(String id, JobStatus jobStatus) {
        MapToJob mapToJob = findMapToJob(id);
        mapToJob.setStatus(jobStatus);
        jobRepository.save(mapToJob);
    }

    public void updateUpdated(String jobId) {
        MapToJob mapToJob = findMapToJob(jobId);
        jobRepository.save(mapToJob);
    }

    @Transactional
    public void setTargetIds(String id, List<String> allMappedIds) {
        MapToJob mapToJob = findMapToJob(id);
        List<MapToResult> targetIds =
                allMappedIds.stream().map(targetId -> new MapToResult(mapToJob, targetId)).toList();
        batchInsertResults(targetIds);
        mapToJob.setTotalTargetIds((long) allMappedIds.size());
        mapToJob.setStatus(JobStatus.FINISHED);
        jobRepository.save(mapToJob);
    }

    public MapToJob createMapToJob(String jobId, MapToJobRequest mapToJobRequest) {
        MapToJob mapToJob = new MapToJob();
        mapToJob.setJobId(jobId);
        mapToJob.setSourceDB(mapToJobRequest.getSource());
        mapToJob.setTargetDB(mapToJobRequest.getTarget());
        mapToJob.setQuery(mapToJobRequest.getQuery());
        mapToJob.setIncludeIsoform(
                Boolean.valueOf(mapToJobRequest.getExtraParams().get(INCLUDE_ISOFORM)));
        mapToJob.setStatus(JobStatus.NEW);
        return createMapToJob(mapToJob);
    }

    public void setErrors(String jobId, ProblemPair error) {
        MapToJob mapToJob = findMapToJob(jobId);
        mapToJob.setStatus(JobStatus.ERROR);
        mapToJob.setError(error);
        jobRepository.save(mapToJob);
    }

    private void batchInsertResults(List<MapToResult> results) {
        String sql = "INSERT INTO map_to_result (map_to_job_id, target_id) VALUES (?, ?)";

        jdbcTemplate.batchUpdate(
                sql,
                results,
                batchSize,
                (ps, result) -> {
                    ps.setLong(1, result.getMapToJob().getId());
                    ps.setString(2, result.getTargetId());
                });
    }
}
