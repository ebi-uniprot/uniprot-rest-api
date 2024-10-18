package org.uniprot.api.async.download.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.*;

import java.util.stream.Stream;

import org.apache.solr.client.solrj.SolrClient;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.uniprot.api.async.download.messaging.repository.DownloadJobRepository;
import org.uniprot.api.async.download.messaging.repository.MapToDownloadJobRepository;
import org.uniprot.api.async.download.model.job.mapto.MapToDownloadJob;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.output.UniProtMediaType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class MapToDownloadControllerIT extends AbstractDownloadControllerIT {

    @Autowired protected MapAsyncConfig mapAsyncConfig;
    @Autowired protected SolrClient solrClient;
    @Autowired protected MapToDownloadJobRepository mapToDownloadJobRepository;
    @Autowired protected MockMvc mockMvc;

    protected void initBeforeAll() throws Exception {
        prepareDownloadFolders();
    }

    @Override
    protected MockMvc getMockMvcObject() {
        return this.mockMvc;
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
    protected MediaType getUnsupportedFormat() {
        return UniProtMediaType.valueOf(OBO_MEDIA_TYPE_VALUE);
    }

    @Override
    protected String getUnsupportedFormatErrorMsg() {
        return "Invalid format received, 'text/plain;format=obo'. Expected one of [text/plain;format=fasta, text/plain;format=tsv, application/json, application/vnd.openxmlformats-officedocument.spreadsheetml.sheet, text/plain;format=list, application/rdf+xml, text/turtle, application/n-triples].";
    }

    @Override
    protected MapToDownloadJob getDownloadJob(
            String jobId,
            String errMsg,
            String query,
            String sort,
            String fields,
            JobStatus jobStatus,
            String format,
            int retried) {
        return MapToDownloadJob.builder()
                .id(jobId)
                .status(jobStatus)
                .error(errMsg)
                .format(format)
                .query(query)
                .sort(sort)
                .fields(fields)
                .retried(retried)
                .build();
    }

    protected DownloadJobRepository getDownloadJobRepository() {
        return this.mapToDownloadJobRepository;
    }

    @Override
    protected TestAsyncConfig getTestAsyncConfig() {
        return mapAsyncConfig;
    }

    @Override
    protected @NotNull String getJobHashAlreadyFinished() {
        return "7a2333c31382de4421a2e103c1d83fcfa730c7f6";
    }

    @Override
    protected @NotNull String getJobHashForForceOnFailedJobAfterMaxRetryWillRunAgain() {
        return "8251d3411850c4410b05de6b7078302a1367d93c";
    }

    @Override
    protected @NotNull String getJobHashForForceOnFailedJobBeforeMaxRetryWillReturnJobRunning() {
        return "f4a367d599f94183aac3d6c61143f6fea0c0f105";
    }
}
