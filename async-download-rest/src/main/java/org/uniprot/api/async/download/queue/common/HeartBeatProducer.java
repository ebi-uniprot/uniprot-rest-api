package org.uniprot.api.async.download.queue.common;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.LongConsumer;

import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.repository.DownloadJobRepository;
import org.uniprot.api.rest.download.configuration.AsyncDownloadHeartBeatConfiguration;
import org.uniprot.api.rest.download.model.DownloadJob;

@Component
@Slf4j
@Profile({"asyncDownload"})
public class HeartBeatProducer {
    private static final String UPDATE_COUNT = "updateCount";
    private static final String UPDATED = "updated";
    private static final String PROCESSED_ENTRIES = "processedEntries";
    // number processed entries so far for a given job id
    private final Map<String, Long> processedEntries = new HashMap<>();
    // number processed entries for a given job id, at the time it was last updated in the cache
    private final Map<String, Long> lastSavedPoints = new HashMap<>();
    private final AsyncDownloadHeartBeatConfiguration asyncDownloadHeartBeatConfiguration;
    private final DownloadJobRepository jobRepository;

    public HeartBeatProducer(
            AsyncDownloadHeartBeatConfiguration asyncDownloadHeartBeatConfiguration,
            DownloadJobRepository jobRepository) {
        this.asyncDownloadHeartBeatConfiguration = asyncDownloadHeartBeatConfiguration;
        this.jobRepository = jobRepository;
    }

    public void createForIds(DownloadJob downloadJob) {
        try {
            createIfEligible(
                    downloadJob,
                    1,
                    asyncDownloadHeartBeatConfiguration.getIdsInterval(),
                    pe -> {
                        long newUpdateCount = downloadJob.getUpdateCount() + 1;
                        downloadJob.setUpdateCount(newUpdateCount);
                        jobRepository.update(
                                downloadJob.getId(),
                                Map.of(UPDATE_COUNT, newUpdateCount, UPDATED, LocalDateTime.now()));
                    });
            log.info(
                    String.format(
                            "%s: Download Job ID: %s was updated in Solr phase",
                            downloadJob.getUpdateCount(), downloadJob.getId()));

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
        if (asyncDownloadHeartBeatConfiguration.isEnabled()) {
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
                    asyncDownloadHeartBeatConfiguration.getResultsInterval(),
                    pe -> {
                        long newUpdateCount = downloadJob.getUpdateCount() + 1;
                        downloadJob.setUpdateCount(newUpdateCount);
                        downloadJob.setProcessedEntries(pe);
                        jobRepository.update(
                                downloadJob.getId(),
                                Map.of(
                                        UPDATE_COUNT, newUpdateCount,
                                        UPDATED, LocalDateTime.now(),
                                        PROCESSED_ENTRIES, pe));
                    });
            log.info(
                    String.format(
                            "%s: Download Job ID: %s was updated in writing phase. Number of  entries processed: %d",
                            downloadJob.getUpdateCount(),
                            downloadJob.getId(),
                            downloadJob.getProcessedEntries()));
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
