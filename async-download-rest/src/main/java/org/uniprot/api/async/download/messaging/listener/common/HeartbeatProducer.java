package org.uniprot.api.async.download.messaging.listener.common;

import java.text.MessageFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.LongConsumer;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.repository.DownloadJobRepository;
import org.uniprot.api.async.download.model.common.DownloadJob;

@Component
@Slf4j
public class HeartbeatProducer {
    private static final String UPDATE_COUNT = "updateCount";
    private static final String UPDATED = "updated";
    private static final String PROCESSED_ENTRIES = "processedEntries";
    private final RetryPolicy<Object> retryPolicy;
    // number processed entries so far for a given job id
    private final Map<String, Long> processedEntries = new HashMap<>();
    // number processed entries for a given job id, at the time it was last updated in the cache
    private final Map<String, Long> lastSavedPoints = new HashMap<>();
    private final HeartbeatConfig heartbeatConfig;
    private final DownloadJobRepository jobRepository;

    public HeartbeatProducer(HeartbeatConfig heartbeatConfig, DownloadJobRepository jobRepository) {
        this.heartbeatConfig = heartbeatConfig;
        this.jobRepository = jobRepository;
        this.retryPolicy =
                new RetryPolicy<>()
                        .handle(Exception.class)
                        .withMaxRetries(heartbeatConfig.getRetryCount())
                        .withDelay(Duration.ofMillis(heartbeatConfig.getRetryDelayInMillis()));
    }

    public void createForIds(DownloadJob downloadJob) {
        try {
            createIfEligible(
                    downloadJob,
                    1,
                    heartbeatConfig.getIdsInterval(),
                    pe -> {
                        long newUpdateCount = downloadJob.getUpdateCount() + 1;
                        downloadJob.setUpdateCount(newUpdateCount);
                        Failsafe.with(retryPolicy)
                                .onFailure(
                                        throwable ->
                                                log.warn(
                                                        MessageFormat.format(
                                                                "Job {0} was failed to update the processed count to {1} in solr phase due to {2}",
                                                                downloadJob.getId(),
                                                                newUpdateCount,
                                                                throwable.getFailure())))
                                .run(
                                        () ->
                                                jobRepository.update(
                                                        downloadJob.getId(),
                                                        Map.of(
                                                                UPDATE_COUNT,
                                                                newUpdateCount,
                                                                UPDATED,
                                                                LocalDateTime.now())));
                        log.info(
                                String.format(
                                        "%s: Download Job ID: %s was updated in Solr phase",
                                        downloadJob.getUpdateCount(), downloadJob.getId()));
                    });

        } catch (Exception e) {
            log.warn(
                    String.format(
                            "%s: Updating Download Job ID: %s in Solr phase was failed, %s",
                            downloadJob.getUpdateCount(),
                            downloadJob.getId(),
                            Arrays.toString(e.getStackTrace())));
        }
    }

    private void createIfEligible(
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

    public void createForResults(DownloadJob downloadJob, long increase) {
        try {
            createIfEligible(
                    downloadJob,
                    increase,
                    heartbeatConfig.getResultsInterval(),
                    pe -> {
                        long newUpdateCount = downloadJob.getUpdateCount() + 1;
                        downloadJob.setUpdateCount(newUpdateCount);
                        downloadJob.setProcessedEntries(pe);
                        Failsafe.with(retryPolicy)
                                .onFailure(
                                        throwable ->
                                                log.warn(
                                                        MessageFormat.format(
                                                                "Job {0} was failed to update the processed count to {1} in writing phase due to {2}",
                                                                downloadJob.getId(),
                                                                newUpdateCount,
                                                                throwable.getFailure())))
                                .run(
                                        () ->
                                                jobRepository.update(
                                                        downloadJob.getId(),
                                                        Map.of(
                                                                UPDATE_COUNT, newUpdateCount,
                                                                UPDATED, LocalDateTime.now(),
                                                                PROCESSED_ENTRIES, pe)));
                        log.info(
                                String.format(
                                        "%s: Download Job ID: %s was updated in writing phase. Number of  entries processed: %d",
                                        downloadJob.getUpdateCount(),
                                        downloadJob.getId(),
                                        downloadJob.getProcessedEntries()));
                    });
        } catch (Exception e) {
            log.warn(
                    String.format(
                            "%s: Updating Download Job ID: %s in writing phase was failed. Number of entries processed: %d, %s",
                            downloadJob.getUpdateCount(),
                            downloadJob.getId(),
                            downloadJob.getProcessedEntries(),
                            Arrays.toString(e.getStackTrace())));
        }
    }

    public void stop(String jobId) {
        processedEntries.remove(jobId);
        lastSavedPoints.remove(jobId);
    }
}
