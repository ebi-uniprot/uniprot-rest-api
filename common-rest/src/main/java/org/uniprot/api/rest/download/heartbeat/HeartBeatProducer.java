package org.uniprot.api.rest.download.heartbeat;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.function.LongConsumer;

import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.download.configuration.AsyncDownloadHeartBeatConfiguration;
import org.uniprot.api.rest.download.model.DownloadJob;
import org.uniprot.api.rest.download.repository.DownloadJobRepository;

@Component
@Slf4j
@Profile({"asyncDownload"})
public class HeartBeatProducer {
    private static final String UPDATE_COUNT = "updateCount";
    private static final String UPDATED = "updated";
    private static final String PROCESSED_ENTRIES = "processedEntries";
    private final Map<String, Long> processedEntries = new HashMap<>();
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
            log.debug(
                    String.format(
                            "%s: Download Job ID: %s was updated in Solr phase",
                            downloadJob.getUpdateCount(), downloadJob.getId()));

        } catch (Exception e) {
            log.warn(
                    String.format(
                            "%s: Updating Download Job ID: %s in Solr phase was failed",
                            downloadJob.getUpdateCount(), downloadJob.getId()));
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
            log.debug(
                    String.format(
                            "%s: Download Job ID: %s was updated in writing phase. Number of  entries processed: %d",
                            downloadJob.getUpdateCount(),
                            downloadJob.getId(),
                            downloadJob.getProcessedEntries()));
        } catch (Exception e) {
            log.warn(
                    String.format(
                            "%s: Updating Download Job ID: %s in writing phase was failed. Number of entries processed: %d",
                            downloadJob.getUpdateCount(),
                            downloadJob.getId(),
                            downloadJob.getProcessedEntries()));
        }
    }

    public void stop(String jobId) {
        processedEntries.remove(jobId);
        lastSavedPoints.remove(jobId);
    }
}
