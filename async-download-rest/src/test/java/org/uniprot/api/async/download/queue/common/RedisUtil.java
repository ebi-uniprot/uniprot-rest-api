package org.uniprot.api.async.download.queue.common;

import java.util.Optional;
import java.util.concurrent.Callable;

import org.springframework.amqp.core.AmqpAdmin;
import org.uniprot.api.async.download.repository.DownloadJobRepository;
import org.uniprot.api.rest.download.model.DownloadJob;
import org.uniprot.api.rest.download.model.JobStatus;

public class RedisUtil {

    public static Callable<Boolean> jobCreatedInRedis(
            DownloadJobRepository downloadJobRepository, String jobId) {
        return () -> downloadJobRepository.existsById(jobId);
    }

    public static Callable<Boolean> jobFinished(
            DownloadJobRepository downloadJobRepository, String jobId) {
        return jobStatus(downloadJobRepository, jobId, JobStatus.FINISHED);
    }

    public static Callable<Boolean> jobErrored(
            DownloadJobRepository downloadJobRepository, String jobId) {
        return jobStatus(downloadJobRepository, jobId, JobStatus.ERROR);
    }

    public static Callable<Boolean> jobUnfinished(
            DownloadJobRepository downloadJobRepository, String jobId) {
        return jobStatus(downloadJobRepository, jobId, JobStatus.UNFINISHED);
    }

    public static Callable<Boolean> jobRetriedMaximumTimes(
            DownloadJobRepository downloadJobRepository, String jobId, int maxRetry) {
        return () -> {
            Optional<DownloadJob> optJob = downloadJobRepository.findById(jobId);
            if (optJob.isPresent()) {
                return optJob.get().getRetried() == maxRetry;
            }
            return false;
        };
    }

    public static Callable<Boolean> verifyJobRetriedCountIsEqualToGivenCount(
            DownloadJobRepository downloadJobRepository, String jobId, int givenCount) {
        return () -> {
            Optional<DownloadJob> optJob = downloadJobRepository.findById(jobId);
            if (optJob.isPresent()) {
                return (optJob.get().getRetried() == givenCount);
            }
            return false;
        };
    }

    public static Callable<Integer> getMessageCountInQueue(AmqpAdmin amqpAdmin, String queueName) {
        return () -> (Integer) amqpAdmin.getQueueProperties(queueName).get("QUEUE_MESSAGE_COUNT");
    }

    public static Callable<Boolean> verifyMessageCountIsThanOrEqualToRejectedCount(
            AmqpAdmin amqpAdmin, String queueName, int rejectedMsgCount) {
        return () -> {
            int count =
                    (Integer) amqpAdmin.getQueueProperties(queueName).get("QUEUE_MESSAGE_COUNT");
            return count >= rejectedMsgCount;
        };
    }

    private static Callable<Boolean> jobStatus(
            DownloadJobRepository downloadJobRepository, String jobId, JobStatus status) {
        return () -> {
            Optional<DownloadJob> optJob = downloadJobRepository.findById(jobId);
            if (optJob.isPresent()) {
                return (optJob.get().getStatus() == status);
            }
            return false;
        };
    }
}
