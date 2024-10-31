package org.uniprot.api.async.download.messaging.consumer.heartbeat;

import static org.uniprot.api.async.download.messaging.repository.JobFields.*;

import java.text.MessageFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.LongConsumer;

import org.uniprot.api.async.download.model.job.DownloadJob;
import org.uniprot.api.async.download.service.JobService;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

@Slf4j
public class HeartbeatProducer {
    private final RetryPolicy<Object> retryPolicy;
    // number processed entries so far for a given job id
    private final Map<String, Long> processedEntries = new HashMap<>();
    // number processed entries for a given job id, at the time it was last updated in the cache
    private final Map<String, Long> lastSavedPoints = new HashMap<>();
    private final HeartbeatConfig heartbeatConfig;
    private final JobService<? extends DownloadJob> jobService;

    public HeartbeatProducer(
            HeartbeatConfig heartbeatConfig, JobService<? extends DownloadJob> jobRepository) {
        this.heartbeatConfig = heartbeatConfig;
        this.jobService = jobRepository;
        this.retryPolicy =
                new RetryPolicy<>()
                        .handle(Exception.class)
                        .withMaxRetries(heartbeatConfig.getRetryCount())
                        .withDelay(Duration.ofMillis(heartbeatConfig.getRetryDelayInMillis()));
    }

    public void generateForIds(String jobId) {
        generateForIds(
                jobService
                        .find(jobId)
                        .orElseThrow(
                                () -> new IllegalArgumentException("Invalid job id: " + jobId)));
    }

    public void generateForFromIds(String jobId) {
        generateForFromIds(
                jobService
                        .find(jobId)
                        .orElseThrow(
                                () -> new IllegalArgumentException("Invalid job id: " + jobId)));
    }

    public void generateForIds(DownloadJob downloadJob) {
        try {
            generateIfEligible(
                    downloadJob,
                    1,
                    heartbeatConfig.getIdsInterval(),
                    pe ->
                            incrementUpdateCount(
                                    downloadJob,
                                    "{0}: Job ID: {1} updated in Solr phase",
                                    "Job {0} failed to update the processed count to {1} in Solr phase due to {2}"));

        } catch (Exception e) {
            log.warn(
                    MessageFormat.format(
                            "{0}: Updating Job ID: {1} in Solr phase failed, {2}",
                            downloadJob.getUpdateCount(),
                            downloadJob.getId(),
                            Arrays.toString(e.getStackTrace())));
        }
    }

    private void incrementUpdateCount(
            DownloadJob downloadJob, String successMessage, String failureMessage) {
        long newUpdateCount = downloadJob.getUpdateCount() + 1;
        downloadJob.setUpdateCount(newUpdateCount);
        Failsafe.with(retryPolicy)
                .onFailure(
                        throwable ->
                                log.warn(
                                        MessageFormat.format(
                                                failureMessage,
                                                downloadJob.getId(),
                                                newUpdateCount,
                                                throwable.getFailure())))
                .run(
                        () ->
                                jobService.update(
                                        downloadJob.getId(),
                                        Map.of(
                                                UPDATE_COUNT.getName(),
                                                newUpdateCount,
                                                UPDATED.getName(),
                                                LocalDateTime.now())));
        log.info(
                MessageFormat.format(
                        successMessage, downloadJob.getUpdateCount(), downloadJob.getId()));
    }

    public void generateForFromIds(DownloadJob downloadJob) {
        try {
            generateIfEligible(
                    downloadJob,
                    downloadJob.getTotalFromIds(),
                    1,
                    heartbeatConfig.getIdsInterval(),
                    pe ->
                            incrementUpdateCount(
                                    downloadJob,
                                    "{0}: Job ID: {1} updated in initial Solr phase",
                                    "Job {0} failed to update the processed count to {1} in initial Solr phase due to {2}"));

        } catch (Exception e) {
            log.warn(
                    MessageFormat.format(
                            "{0}: Updating Job ID: {1} in initial Solr phase failed, {2}",
                            downloadJob.getUpdateCount(),
                            downloadJob.getId(),
                            Arrays.toString(e.getStackTrace())));
        }
    }

    private void generateIfEligible(
            DownloadJob downloadJob, long size, long interval, LongConsumer consumer) {
        if (heartbeatConfig.isEnabled()) {
            String jobId = downloadJob.getId();
            long totalProcessedEntries = processedEntries.getOrDefault(jobId, 0L) + size;
            processedEntries.put(jobId, totalProcessedEntries);
            if (isEligibleToUpdate(
                    downloadJob.getTotalEntries(),
                    totalProcessedEntries,
                    lastSavedPoints.getOrDefault(jobId, 0L),
                    interval)) {
                consumer.accept(totalProcessedEntries);
                lastSavedPoints.put(jobId, totalProcessedEntries);
            }
        }
    }

    private void generateIfEligible(
            DownloadJob downloadJob,
            long totalNumberOfEntries,
            long size,
            long interval,
            LongConsumer consumer) {
        if (heartbeatConfig.isEnabled()) {
            String jobId = downloadJob.getId();
            long totalProcessedEntries = processedEntries.getOrDefault(jobId, 0L) + size;
            processedEntries.put(jobId, totalProcessedEntries);
            if (isEligibleToUpdate(
                    totalNumberOfEntries,
                    totalProcessedEntries,
                    lastSavedPoints.getOrDefault(jobId, 0L),
                    interval)) {
                consumer.accept(totalProcessedEntries);
                lastSavedPoints.put(jobId, totalProcessedEntries);
            }
        }
    }

    private boolean isEligibleToUpdate(
            long totalEntries,
            long totalNumberOfProcessedEntries,
            long lastSavedPoint,
            long interval) {
        // example, batchSize=70, interval=50
        // first update is 70
        // next checkPoint = 70 - (70%50) + 50 = 100
        long nextCheckPoint = lastSavedPoint - (lastSavedPoint % interval) + interval;
        return totalNumberOfProcessedEntries >= Math.min(totalEntries, nextCheckPoint);
    }

    public void generateForResults(DownloadJob downloadJob, long increase) {
        try {
            generateIfEligible(
                    downloadJob,
                    increase,
                    heartbeatConfig.getResultsInterval(),
                    pe -> incrementUpdateAndProcessedCounts(downloadJob, pe));
        } catch (Exception e) {
            log.warn(
                    MessageFormat.format(
                            "{0}: Updating Job ID: {1} in Voldemort phase failed. Entries processed: {2}, {3}",
                            downloadJob.getUpdateCount(),
                            downloadJob.getId(),
                            downloadJob.getProcessedEntries(),
                            Arrays.toString(e.getStackTrace())));
        }
    }

    private void incrementUpdateAndProcessedCounts(DownloadJob downloadJob, long pe) {
        long newUpdateCount = downloadJob.getUpdateCount() + 1;
        downloadJob.setUpdateCount(newUpdateCount);
        downloadJob.setProcessedEntries(pe);
        Failsafe.with(retryPolicy)
                .onFailure(
                        throwable ->
                                log.warn(
                                        MessageFormat.format(
                                                "Job ID {0} failed to update the processed count to {1} in Voldemort phase due to {2}",
                                                downloadJob.getId(),
                                                newUpdateCount,
                                                throwable.getFailure())))
                .run(
                        () ->
                                jobService.update(
                                        downloadJob.getId(),
                                        Map.of(
                                                UPDATE_COUNT.getName(), newUpdateCount,
                                                UPDATED.getName(), LocalDateTime.now(),
                                                PROCESSED_ENTRIES.getName(), pe)));
        log.info(
                MessageFormat.format(
                        "{0}: Job ID: {1} updated in Voldemort phase. Entries processed: {2}",
                        downloadJob.getUpdateCount(),
                        downloadJob.getId(),
                        downloadJob.getProcessedEntries()));
    }

    public void stop(String jobId) {
        processedEntries.remove(jobId);
        lastSavedPoints.remove(jobId);
    }
}
