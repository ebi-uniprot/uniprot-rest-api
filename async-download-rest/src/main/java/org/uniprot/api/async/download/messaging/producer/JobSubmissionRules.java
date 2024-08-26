package org.uniprot.api.async.download.messaging.producer;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Optional;

import org.uniprot.api.async.download.model.JobSubmitFeedback;
import org.uniprot.api.async.download.model.job.DownloadJob;
import org.uniprot.api.async.download.model.request.DownloadRequest;
import org.uniprot.api.async.download.service.JobService;
import org.uniprot.api.rest.download.model.JobStatus;

public abstract class JobSubmissionRules<T extends DownloadRequest, R extends DownloadJob> {
    private final int maxRetryCount;
    private final int maxWaitingTime;
    private final JobService<R> jobService;

    protected JobSubmissionRules(int maxRetryCount, int maxWaitingTime, JobService<R> jobService) {
        this.maxRetryCount = maxRetryCount;
        this.maxWaitingTime = maxWaitingTime;
        this.jobService = jobService;
    }

    public JobSubmitFeedback submit(T request) {
        String jobId = request.getDownloadJobId();
        Optional<? extends DownloadJob> downloadJobOpt = jobService.find(jobId);
        if (downloadJobOpt.isPresent()) {
            DownloadJob downloadJob = downloadJobOpt.get();
            JobStatus downloadJobStatus = downloadJob.getStatus();
            if (!request.isForce()) {
                return new JobSubmitFeedback(
                        false, String.format("Job with id %s has already been submitted", jobId));
            } else {
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
