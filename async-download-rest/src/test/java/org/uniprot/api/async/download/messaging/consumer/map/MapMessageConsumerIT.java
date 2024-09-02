package org.uniprot.api.async.download.messaging.consumer.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.download.model.JobStatus.FINISHED;
import static org.uniprot.api.rest.output.UniProtMediaType.*;

import java.util.Objects;
import java.util.stream.Stream;

import org.apache.solr.client.solrj.SolrClient;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.beans.factory.annotation.Autowired;
import org.uniprot.api.async.download.controller.MapAsyncConfig;
import org.uniprot.api.async.download.controller.TestAsyncConfig;
import org.uniprot.api.async.download.messaging.config.map.MapDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.consumer.MessageConsumer;
import org.uniprot.api.async.download.messaging.consumer.SolrIdMessageConsumerIT;
import org.uniprot.api.async.download.messaging.repository.MapDownloadJobRepository;
import org.uniprot.api.async.download.messaging.result.map.MapFileHandler;
import org.uniprot.api.async.download.model.job.map.MapDownloadJob;
import org.uniprot.api.async.download.model.request.map.MapDownloadRequest;
import org.uniprot.api.rest.download.model.JobStatus;

public abstract class MapMessageConsumerIT<T extends MapDownloadRequest>
        extends SolrIdMessageConsumerIT<T, MapDownloadJob> {
    @Autowired protected MapMessageConsumer mapMessageConsumer;
    @Autowired protected SolrClient solrClient;
    @Autowired protected MapAsyncConfig mapAsyncConfig;
    @Autowired protected MapDownloadJobRepository mapDownloadJobRepository;
    @Autowired protected MapDownloadConfigProperties mapDownloadConfigProperties;
    @Autowired protected MapFileHandler mapFileHandler;

    protected void initBeforeAll() throws Exception {
        prepareDownloadFolders();
    }

    void initBeforeEach() {
        messageConsumer = (MessageConsumer<T, MapDownloadJob>) mapMessageConsumer;
        fileHandler = mapFileHandler;
        downloadJobRepository = mapDownloadJobRepository;
        downloadConfigProperties = mapDownloadConfigProperties;
    }

    @Override
    protected void areFilesPresent() {
        fileHandler.areAllFilesPresent(ID);
    }

    @Override
    protected TestAsyncConfig getTestAsyncConfig() {
        return mapAsyncConfig;
    }

    @Override
    protected void assertJobSpecifics(MapDownloadJob job, String format) {
        assertEquals(12, job.getProcessedEntries());
        assertEquals(FINISHED, job.getStatus());
        assertEquals(Objects.equals(format, LIST_MEDIA_TYPE_VALUE) ? 18 : 13, job.getUpdateCount());
    }

    @Override
    protected void saveDownloadJob(
            String id,
            int retryCount,
            JobStatus jobStatus,
            long updateCount,
            long processedEntries) {
        mapDownloadJobRepository.save(
                MapDownloadJob.builder()
                        .id(id)
                        .status(jobStatus)
                        .updateCount(updateCount)
                        .processedEntries(processedEntries)
                        .retried(retryCount)
                        .build());
        System.out.println();
    }

    @Override
    protected Stream<Arguments> getSupportedFormats() {
        return Stream.of(
                        "json",
                        FASTA_MEDIA_TYPE_VALUE,
                        TSV_MEDIA_TYPE_VALUE,
                        APPLICATION_JSON_VALUE,
                        XLS_MEDIA_TYPE_VALUE,
                        LIST_MEDIA_TYPE_VALUE,
                        RDF_MEDIA_TYPE_VALUE,
                        TURTLE_MEDIA_TYPE_VALUE,
                        N_TRIPLES_MEDIA_TYPE_VALUE)
                .map(Arguments::of);
    }

    @Override
    protected boolean isMappedDownload() {
        return true;
    }
}
