package org.uniprot.api.async.download.messaging.producer.common;

import org.uniprot.api.async.download.model.common.DownloadJob;
import org.uniprot.api.async.download.model.common.JobSubmitFeedback;
import org.uniprot.api.async.download.refactor.request.DownloadRequest;
import org.uniprot.api.async.download.refactor.service.JobService;
import org.uniprot.api.rest.download.model.JobStatus;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Optional;

public abstract class AsyncDownloadSubmissionRules<T extends DownloadRequest, R extends DownloadJob> {
    private final int maxRetryCount;
    private final int maxWaitingTime;
    private final JobService<R> downloadJobRepository;

    protected AsyncDownloadSubmissionRules(
            int maxRetryCount, int maxWaitingTime, JobService<R> downloadJobRepository) {
        this.maxRetryCount = maxRetryCount;
        this.maxWaitingTime = maxWaitingTime;
        this.downloadJobRepository = downloadJobRepository;
    }

    public JobSubmitFeedback submit(T request) {
        String id = request.getId();
        Optional<? extends DownloadJob> downloadJobOpt = downloadJobRepository.find(id);
        if (downloadJobOpt.isPresent()) {
            DownloadJob downloadJob = downloadJobOpt.get();
            if (!request.isForce()) {
                return new JobSubmitFeedback(
                        false, String.format("Job with id %s has already been submitted", id));
            } else {
                JobStatus downloadJobStatus = downloadJob.getStatus();

                if (EnumSet.of(JobStatus.NEW, JobStatus.UNFINISHED).contains(downloadJobStatus)) {
                    return new JobSubmitFeedback(
                            false,
                            String.format("Job with id %s has already been submitted", id));
                }
                if (JobStatus.ABORTED.equals(downloadJobStatus)) {
                    return new JobSubmitFeedback(
                            false,
                            String.format(
                                    "Job with id %s is already aborted for the excess size of results",
                                    id));
                }
                if (JobStatus.FINISHED.equals(downloadJobStatus)) {
                    return new JobSubmitFeedback(
                            false,
                            String.format(
                                    "Job with id %s has already been finished successfully.",
                                    id));
                }
                if (JobStatus.ERROR.equals(downloadJobStatus)
                        && maxRetryCountNotFinished(downloadJob)) {
                    return new JobSubmitFeedback(
                            false, String.format("Job with id %s is already being retried", id));
                }
                if (EnumSet.of(JobStatus.RUNNING, JobStatus.PROCESSING).contains(downloadJobStatus)
                        && maxWaitingTimeNotElapsed(downloadJob)) {
                    return new JobSubmitFeedback(
                            false,
                            String.format("Job with id %s is already running and live", id));
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
