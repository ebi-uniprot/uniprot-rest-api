package org.uniprot.api.async.download.queue.common;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Optional;

import org.uniprot.api.async.download.model.DownloadJob;
import org.uniprot.api.async.download.model.JobSubmitFeedback;
import org.uniprot.api.async.download.repository.DownloadJobRepository;
import org.uniprot.api.rest.download.model.JobStatus;

public abstract class AsyncDownloadSubmissionRules {
    private final int maxRetryCount;
    private final int maxWaitingTime;
    private final DownloadJobRepository downloadJobRepository;

    public AsyncDownloadSubmissionRules(
            int maxRetryCount, int maxWaitingTime, DownloadJobRepository downloadJobRepository) {
        this.maxRetryCount = maxRetryCount;
        this.maxWaitingTime = maxWaitingTime;
        this.downloadJobRepository = downloadJobRepository;
    }

    public JobSubmitFeedback submit(String jobId, boolean force) {
        Optional<DownloadJob> downloadJobOpt = downloadJobRepository.findById(jobId);
        if (downloadJobOpt.isPresent()) {
            DownloadJob downloadJob = downloadJobOpt.get();
            if (!force) {
                return new JobSubmitFeedback(
                        false, String.format("Job with id %s has already been submitted", jobId));
            } else {
                JobStatus downloadJobStatus = downloadJob.getStatus();

                if (EnumSet.of(JobStatus.NEW, JobStatus.UNFINISHED).contains(downloadJobStatus)) {
                    return new JobSubmitFeedback(
                            false,
                            String.format("Job with id %s has already been submitted", jobId));
                }
                if (JobStatus.ABORTED.equals(downloadJobStatus)) {
                    return new JobSubmitFeedback(
                            false,
                            String.format(
                                    "Job with id %s is already aborted for the excess size of results",
                                    jobId));
                }
                if (JobStatus.FINISHED.equals(downloadJobStatus)) {
                    return new JobSubmitFeedback(
                            false,
                            String.format(
                                    "Job with id %s has already been finished successfully.",
                                    jobId));
                }
                if (JobStatus.ERROR.equals(downloadJobStatus)
                        && maxRetryCountNotFinished(downloadJob)) {
                    return new JobSubmitFeedback(
                            false, String.format("Job with id %s is already being retried", jobId));
                }
                if (EnumSet.of(JobStatus.RUNNING, JobStatus.PROCESSING).contains(downloadJobStatus)
                        && maxWaitingTimeNotElapsed(downloadJob)) {
                    return new JobSubmitFeedback(
                            false,
                            String.format("Job with id %s is already running and live", jobId));
                }
            }
        }
        return new JobSubmitFeedback(true);
    }

    private boolean maxRetryCountNotFinished(DownloadJob downloadJob) {
        return downloadJob.getRetried() < maxRetryCount;
    }

    private boolean maxWaitingTimeNotElapsed(DownloadJob downloadJob) {
        return LocalDateTime.now().isBefore(downloadJob.getUpdated().plusMinutes(maxWaitingTime));
    }
}
