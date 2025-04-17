package org.uniprot.api.rest.output.job;

import java.time.LocalDateTime;
import java.util.List;

import org.uniprot.api.common.repository.search.ProblemPair;
import org.uniprot.api.rest.download.model.JobStatus;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * @author sahmad
 * @created 22/02/2021
 */
@Getter
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class JobStatusResponse {
    private final JobStatus jobStatus;
    private final List<ProblemPair> warnings;
    private final List<ProblemPair> errors;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime start;

    private Long totalEntries;
    private Long processedEntries;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastUpdated;

    public JobStatusResponse(JobStatus jobStatus) {
        this(jobStatus, List.of());
    }

    public JobStatusResponse(List<ProblemPair> errors) {
        this(JobStatus.ERROR, List.of(), errors);
    }

    public JobStatusResponse(
            List<ProblemPair> errors,
            LocalDateTime start,
            Long totalEntries,
            Long processedEntries,
            LocalDateTime lastUpdated) {
        this(JobStatus.ERROR, List.of(), errors);
        this.start = start;
        this.totalEntries = totalEntries;
        this.processedEntries = processedEntries;
        this.lastUpdated = lastUpdated;
    }

    public JobStatusResponse(JobStatus jobStatus, List<ProblemPair> warnings) {
        this(jobStatus, warnings, List.of());
    }

    public JobStatusResponse(
            JobStatus jobStatus, List<ProblemPair> warnings, List<ProblemPair> errors) {
        this.jobStatus = jobStatus;
        this.warnings = warnings;
        this.errors = errors;
    }

    public JobStatusResponse(
            JobStatus jobStatus,
            List<ProblemPair> warnings,
            List<ProblemPair> errors,
            LocalDateTime start,
            Long totalEntries,
            Long processedEntries,
            LocalDateTime lastUpdated) {
        this(jobStatus, warnings, errors);
        this.start = start;
        this.totalEntries = totalEntries;
        this.processedEntries = processedEntries;
        this.lastUpdated = lastUpdated;
    }

    public JobStatusResponse(
            JobStatus jobStatus,
            LocalDateTime start,
            Long totalEntries,
            Long processedEntries,
            LocalDateTime lastUpdated) {
        this(jobStatus);
        this.start = start;
        this.totalEntries = totalEntries;
        this.processedEntries = processedEntries;
        this.lastUpdated = lastUpdated;
    }

    public JobStatusResponse(JobStatus jobStatus, LocalDateTime start, Long totalEntries, LocalDateTime updated) {
        this(jobStatus);
        this.start = start;
        this.totalEntries = totalEntries;
        this.lastUpdated = updated;
    }
}
