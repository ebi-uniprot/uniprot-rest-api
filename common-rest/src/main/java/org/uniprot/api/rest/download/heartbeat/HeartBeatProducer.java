package org.uniprot.api.rest.download.heartbeat;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.download.configuration.AsyncDownloadHeartBeatConfiguration;
import org.uniprot.api.rest.download.model.DownloadJob;
import org.uniprot.api.rest.download.repository.DownloadJobRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class HeartBeatProducer {
    private final Map<String, Long> downloadJobCheckPoints = new HashMap<>();
    private final AsyncDownloadHeartBeatConfiguration asyncDownloadHeartBeatConfiguration;
    private final DownloadJobRepository jobRepository;

    public HeartBeatProducer(
            AsyncDownloadHeartBeatConfiguration asyncDownloadHeartBeatConfiguration,
            DownloadJobRepository jobRepository) {
        this.asyncDownloadHeartBeatConfiguration = asyncDownloadHeartBeatConfiguration;
        this.jobRepository = jobRepository;
    }

    public void create(DownloadJob downloadJob, long size) {
        try {
            if (asyncDownloadHeartBeatConfiguration.isEnabled()) {
                String jobId = downloadJob.getId();
                long totalNumberOfProcessedEntries =
                        downloadJobCheckPoints.getOrDefault(jobId, 0L) + size;
                downloadJobCheckPoints.put(jobId, totalNumberOfProcessedEntries);
                if (isNextCheckPointPassed(downloadJob, totalNumberOfProcessedEntries)) {
                    downloadJob.setEntriesProcessed(totalNumberOfProcessedEntries);
                    downloadJob.setUpdated(LocalDateTime.now());
                    jobRepository.save(downloadJob);
                }
            }
        } catch (Exception e) {
            log.warn(
                    String.format(
                            "Updating the Number of Processed Entries was failed for Download Job ID: %s , "
                                    + "Last updated number of entries processed: %d",
                            downloadJob.getId(), downloadJob.getEntriesProcessed()));
        }
    }

    public void stop(String jobId) {
        downloadJobCheckPoints.remove(jobId);
    }

    private boolean isNextCheckPointPassed(
            DownloadJob downloadJob, long totalNumberOfProcessedEntries) {
        long nextCheckPoint =
                downloadJob.getEntriesProcessed() - downloadJob.getEntriesProcessed()% asyncDownloadHeartBeatConfiguration.getInterval()
                        + asyncDownloadHeartBeatConfiguration.getInterval();
        long totalNumberOfEntries = downloadJob.getTotalEntries();
        return totalNumberOfProcessedEntries >= Math.min(totalNumberOfEntries, nextCheckPoint);
    }
}
