package org.uniprot.api.rest.download.heartbeat;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.download.configuration.AsyncDownloadHeartBeatConfiguration;
import org.uniprot.api.rest.download.model.DownloadJob;
import org.uniprot.api.rest.download.repository.DownloadJobRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.function.LongConsumer;

@Component
@Slf4j
@Profile({"asyncDownload"})
public class HeartBeatProducer {
    private static final String UPDATE_COUNT = "updateCount";
    private static final String UPDATED = "updated";
    private static final String PROCESSED_ENTRIES = "processedEntries";
    private final Map<String, Long> downloadJobCheckPoints = new HashMap<>();
    private final AsyncDownloadHeartBeatConfiguration asyncDownloadHeartBeatConfiguration;
    private final DownloadJobRepository jobRepository;

    public HeartBeatProducer(
            AsyncDownloadHeartBeatConfiguration asyncDownloadHeartBeatConfiguration,
            DownloadJobRepository jobRepository) {
        this.asyncDownloadHeartBeatConfiguration = asyncDownloadHeartBeatConfiguration;
        this.jobRepository = jobRepository;
    }

    public void create(DownloadJob downloadJob) {
        try {
            createIfEligible(
                    downloadJob,
                    1,
                    pe -> {
                        long newUpdateCount = downloadJob.getUpdateCount() + 1;
                        downloadJob.setUpdateCount(newUpdateCount);
                        jobRepository.update(
                                downloadJob.getId(),
                                Map.of(
                                        UPDATE_COUNT,
                                        newUpdateCount,
                                        UPDATED,
                                        LocalDateTime.now()));
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

    private void createIfEligible(DownloadJob downloadJob, long size, LongConsumer consumer) {
        if (asyncDownloadHeartBeatConfiguration.isEnabled()) {
            long totalNumberOfProcessedEntries =
                    downloadJobCheckPoints.getOrDefault(downloadJob.getId(), 0L) + size;
            if (isEligibleToUpdate(downloadJob, totalNumberOfProcessedEntries)) {
                consumer.accept(totalNumberOfProcessedEntries);
            }
        }
    }

    private boolean isEligibleToUpdate(
            DownloadJob downloadJob, long totalNumberOfProcessedEntries) {
        String jobId = downloadJob.getId();
        downloadJobCheckPoints.put(jobId, totalNumberOfProcessedEntries);
        long nextCheckPoint =
                downloadJob.getProcessedEntries()
                        - (downloadJob.getProcessedEntries()
                                % asyncDownloadHeartBeatConfiguration.getInterval())
                        + asyncDownloadHeartBeatConfiguration.getInterval();
        return totalNumberOfProcessedEntries
                >= Math.min(downloadJob.getTotalEntries(), nextCheckPoint);
    }

    public void createWithProgress(DownloadJob downloadJob, long increase) {
        try {
            createIfEligible(
                    downloadJob,
                    increase,
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
        downloadJobCheckPoints.remove(jobId);
    }
}