package org.uniprot.api.async.download.messaging.consumer.uniparc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.uniprot.api.rest.download.model.JobStatus.FINISHED;
import static org.uniprot.api.rest.output.UniProtMediaType.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.solr.client.solrj.SolrClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.async.download.AsyncDownloadRestApp;
import org.uniprot.api.async.download.common.UniParcAsyncDownloadUtils;
import org.uniprot.api.async.download.controller.TestAsyncConfig;
import org.uniprot.api.async.download.controller.UniParcAsyncConfig;
import org.uniprot.api.async.download.messaging.config.common.RedisConfiguration;
import org.uniprot.api.async.download.messaging.config.uniparc.UniParcDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.consumer.SolrIdMessageConsumerIT;
import org.uniprot.api.async.download.messaging.repository.UniParcDownloadJobRepository;
import org.uniprot.api.async.download.messaging.result.uniparc.UniParcFileHandler;
import org.uniprot.api.async.download.model.job.uniparc.UniParcDownloadJob;
import org.uniprot.api.async.download.model.request.uniparc.UniParcDownloadRequest;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.rest.controller.ControllerITUtils;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.api.uniparc.common.repository.UniParcDataStoreTestConfig;
import org.uniprot.api.uniparc.common.repository.search.UniParcQueryRepository;
import org.uniprot.api.uniprotkb.common.repository.UniProtKBDataStoreTestConfig;
import org.uniprot.core.uniparc.UniParcEntryLight;
import org.uniprot.core.uniparc.impl.UniParcCrossReferencePair;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.search.SolrCollection;

import com.jayway.jsonpath.JsonPath;

@ActiveProfiles(profiles = {"offline", "idmapping"})
@ContextConfiguration(
        classes = {
            UniParcDataStoreTestConfig.class,
            UniProtKBDataStoreTestConfig.class,
            AsyncDownloadRestApp.class,
            ErrorHandlerConfig.class,
            RedisConfiguration.class
        })
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UniParcMessageConsumerIT
        extends SolrIdMessageConsumerIT<UniParcDownloadRequest, UniParcDownloadJob> {
    @Autowired private UniParcMessageConsumer uniParcMessageConsumer;
    @Autowired private UniParcQueryRepository uniParcQueryRepository;
    @Autowired private SolrClient solrClient;

    @Autowired private UniProtStoreClient<UniParcEntryLight> uniParcLightStoreClient;

    @Autowired private UniProtStoreClient<UniParcCrossReferencePair> xrefStoreClient;

    @Autowired private UniParcAsyncConfig uniParcAsyncConfig;
    @Autowired private UniParcDownloadJobRepository uniParcDownloadJobRepository;
    @Autowired private UniParcDownloadConfigProperties uniParcDownloadConfigProperties;
    @Autowired private UniParcFileHandler uniParcAsyncDownloadFileHandler;

    @Qualifier("uniParcFacetTupleStreamTemplate")
    @Autowired
    private FacetTupleStreamTemplate facetTupleStreamTemplate;

    @Autowired
    @Qualifier("uniParcTupleStreamTemplate")
    private TupleStreamTemplate tupleStreamTemplate;

    @MockBean(name = "uniParcRdfRestTemplate")
    private RestTemplate restTemplate;

    @BeforeAll
    void beforeAll() throws Exception {
        prepareDownloadFolders();
        UniParcAsyncDownloadUtils.saveEntriesInSolrAndStore(
                uniParcQueryRepository,
                cloudSolrClient,
                solrClient,
                uniParcLightStoreClient,
                xrefStoreClient);
    }

    @BeforeEach
    void setUp() {
        fileHandler = uniParcAsyncDownloadFileHandler;
        downloadJobRepository = uniParcDownloadJobRepository;
        messageConsumer = uniParcMessageConsumer;
        downloadConfigProperties = uniParcDownloadConfigProperties;
        downloadRequest = new UniParcDownloadRequest();
        downloadRequest.setFormat("json");
        downloadRequest.setQuery("Human");
        downloadRequest.setFields("upi,length,organism");
        // todo pass actual id
        ControllerITUtils.mockRestTemplateResponsesForRDFFormats(restTemplate, "uniparc");
    }

    @Override
    protected TestAsyncConfig getTestAsyncConfig() {
        return uniParcAsyncConfig;
    }

    @Override
    protected List<SolrCollection> getSolrCollections() {
        return List.of(SolrCollection.uniparc);
    }

    @Override
    protected TupleStreamTemplate getTupleStreamTemplate() {
        return tupleStreamTemplate;
    }

    @Override
    protected FacetTupleStreamTemplate getFacetTupleStreamTemplate() {
        return facetTupleStreamTemplate;
    }

    @Override
    protected void verifyIdsFiles(String id) throws Exception {
        Path idsFilePath = Path.of(getTestAsyncConfig().getIdsFolder() + "/" + id);
        assertTrue(Files.exists(idsFilePath));
        List<String> ids = Files.readAllLines(idsFilePath);
        assertNotNull(ids);
        assertTrue(
                ids.containsAll(
                        List.of(
                                "upi101", "upi202", "upi303", "upi404", "upi505", "upi606",
                                "upi707", "upi808", "upi909", "upi1010", "upi1111", "upi1212")));
    }

    @Override
    protected void verifyResultFile(String id) throws Exception {
        String fileWithExt = id + FileType.GZIP.getExtension();
        Path resultFilePath = Path.of(getTestAsyncConfig().getResultFolder() + "/" + fileWithExt);
        assertTrue(Files.exists(resultFilePath));
        // uncompress the gz file
        Path unzippedFile = Path.of(getTestAsyncConfig().getResultFolder() + "/" + id);
        uncompressFile(resultFilePath, unzippedFile);
        assertTrue(Files.exists(unzippedFile));
        String resultsJson = Files.readString(unzippedFile);
        List<String> ids = JsonPath.read(resultsJson, "$.results.*.uniParcId");
        assertEquals(12, ids.size());
        List<Map<String, Integer>> sequences = JsonPath.read(resultsJson, "$.results.*.sequence");
        sequences.forEach(s -> assertTrue(s.containsKey("length")));
        assertEquals(12, sequences.size());
        List<Map<String, Integer>> organisms = JsonPath.read(resultsJson, "$.results.*.organisms");
        assertEquals(12, organisms.size());
        List<String> mostRecentCrossRefUpdated =
                JsonPath.read(resultsJson, "$.results.*.mostRecentCrossRefUpdated");
        assertEquals(12, mostRecentCrossRefUpdated.size());
        List<String> oldestCrossRefCreated =
                JsonPath.read(resultsJson, "$.results.*.oldestCrossRefCreated");
        assertEquals(12, oldestCrossRefCreated.size());
    }

    @Override
    protected void assertJobSpecifics(UniParcDownloadJob job, String format) {
        assertEquals(12, job.getProcessedEntries());
        assertEquals(FINISHED, job.getStatus());
        assertEquals(Objects.equals(format, LIST_MEDIA_TYPE_VALUE) ? 12 : 7, job.getUpdateCount());
    }

    @Override
    protected void saveDownloadJob(
            String id,
            int retryCount,
            JobStatus jobStatus,
            long updateCount,
            long processedEntries) {
        uniParcDownloadJobRepository.save(
                UniParcDownloadJob.builder()
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
                        APPLICATION_XML_VALUE,
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
}
