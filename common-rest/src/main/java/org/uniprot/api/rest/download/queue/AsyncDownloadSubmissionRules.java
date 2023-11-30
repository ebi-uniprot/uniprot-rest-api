package org.uniprot.api.rest.download.queue;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.download.model.DownloadJob;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.download.repository.DownloadJobRepository;
import org.uniprot.api.rest.output.job.JobSubmitFeedback;

@Component
public class AsyncDownloadSubmissionRules {
    private final int maxRetryCount;
    private final int maxWaitingTime;
    private final DownloadJobRepository downloadJobRepository;

    public AsyncDownloadSubmissionRules(
            @Value("${async.download.retryMaxCount}") int maxRetryCount,
            @Value("${async.download.waitingMaxTime}") int maxWaitingTime,
            DownloadJobRepository downloadJobRepository) {
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

                if (JobStatus.NEW.equals(downloadJobStatus)) {
                    return new JobSubmitFeedback(
                            false,
                            String.format(
                                    "Resubmission is not allowed. Job with id %s is already ready for processing",
                                    jobId));
                }
                if (JobStatus.UNFINISHED.equals(downloadJobStatus)) {
                    return new JobSubmitFeedback(
                            false,
                            String.format(
                                    "Resubmission is not allowed. Job with id %s is already ready for processing for embeddings",
                                    jobId));
                }
                if (JobStatus.ABORTED.equals(downloadJobStatus)) {
                    return new JobSubmitFeedback(
                            false,
                            String.format(
                                    "Resubmission is not allowed. Job with id %s is already aborted for excess size for embeddings",
                                    jobId));
                }
                if (JobStatus.FINISHED.equals(downloadJobStatus)) {
                    return new JobSubmitFeedback(
                            false,
                            String.format(
                                    "Resubmission is not allowed. Job with id %s is already finished successfully. You can already see the file at %s",
                                    jobId, downloadJob.getResultFile()));
                }
                if (JobStatus.ERROR.equals(downloadJobStatus)
                        && downloadJob.getRetried() >= maxRetryCount) {
                    return new JobSubmitFeedback(
                            false,
                            String.format(
                                    "Resubmission is not allowed. Job with id %s is already  being retried",
                                    jobId));
                }
                if (JobStatus.RUNNING.equals(downloadJobStatus)
                        && maxWaitingTimeNotElapsed(downloadJob)) {
                    return new JobSubmitFeedback(
                            false,
                            String.format(
                                    "Resubmission is not allowed. Job with id %s is already running and live",
                                    jobId));
                }
                if (JobStatus.PROCESSING.equals(downloadJobStatus)
                        && maxWaitingTimeNotElapsed(downloadJob)) {
                    return new JobSubmitFeedback(
                            false,
                            String.format(
                                    "Resubmission is not allowed. Job with id %s is already running the embeddings and live",
                                    jobId));
                }
            }
        }
        return new JobSubmitFeedback(true);
    }

    private boolean maxWaitingTimeNotElapsed(DownloadJob downloadJob) {
        return LocalDateTime.now().isBefore(downloadJob.getUpdated().plusMinutes(maxWaitingTime));
    }
}
