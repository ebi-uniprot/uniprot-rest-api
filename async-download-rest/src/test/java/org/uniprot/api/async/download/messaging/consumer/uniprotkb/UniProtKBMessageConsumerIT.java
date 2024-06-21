package org.uniprot.api.async.download.messaging.consumer.uniprotkb;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.download.model.JobStatus.*;
import static org.uniprot.api.rest.output.UniProtMediaType.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.solr.client.solrj.SolrClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.async.download.AsyncDownloadRestApp;
import org.uniprot.api.async.download.controller.TestAsyncConfig;
import org.uniprot.api.async.download.controller.UniProtKBAsyncConfig;
import org.uniprot.api.async.download.messaging.config.common.RedisConfiguration;
import org.uniprot.api.async.download.messaging.config.uniprotkb.UniProtKBDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.config.uniprotkb.embeddings.EmbeddingsQueueConfigProperties;
import org.uniprot.api.async.download.messaging.consumer.SolrIdMessageConsumerIT;
import org.uniprot.api.async.download.messaging.repository.UniProtKBDownloadJobRepository;
import org.uniprot.api.async.download.messaging.result.uniprotkb.UniProtKBAsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.job.uniprotkb.UniProtKBDownloadJob;
import org.uniprot.api.async.download.model.request.uniprotkb.UniProtKBDownloadRequest;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.common.repository.stream.store.uniprotkb.TaxonomyLineageRepository;
import org.uniprot.api.idmapping.common.service.TestConfig;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.api.uniprotkb.common.repository.UniProtKBDataStoreTestConfig;
import org.uniprot.api.uniprotkb.common.repository.search.UniprotQueryRepository;
import org.uniprot.api.uniprotkb.common.utils.UniProtKBAsyncDownloadUtils;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.search.SolrCollection;

import com.jayway.jsonpath.JsonPath;

@ActiveProfiles(profiles = {"offline", "idmapping"})
@ContextConfiguration(
        classes = {
            TestConfig.class,
            UniProtKBDataStoreTestConfig.class,
            UniProtKBDataStoreTestConfig.class,
            AsyncDownloadRestApp.class,
            ErrorHandlerConfig.class,
            RedisConfiguration.class
        })
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UniProtKBMessageConsumerIT
        extends SolrIdMessageConsumerIT<UniProtKBDownloadRequest, UniProtKBDownloadJob> {
    public static final int MAX_ENTRY_COUNT = 15;
    @Autowired private UniProtKBContentBasedAndRetriableMessageConsumer uniProtKBMessageConsumer;
    @Autowired private UniprotQueryRepository uniProtKBQueryRepository;
    @Autowired private SolrClient solrClient;

    @Autowired()
    @Qualifier("uniProtStoreClient")
    private UniProtStoreClient<UniProtKBEntry> storeClient;

    @Autowired private UniProtKBAsyncConfig uniProtKBAsyncConfig;
    @Autowired private UniProtKBDownloadJobRepository uniProtKBDownloadJobRepository;
    @Autowired private UniProtKBDownloadConfigProperties uniProtKBDownloadConfigProperties;
    @Autowired private UniProtKBAsyncDownloadFileHandler uniProtKBAsyncDownloadFileHandler;

    @Qualifier("uniProtKBFacetTupleStreamTemplate")
    @Autowired
    private FacetTupleStreamTemplate facetTupleStreamTemplate;

    @Autowired
    @Qualifier("uniProtKBTupleStream")
    private TupleStreamTemplate tupleStreamTemplate;

    @MockBean(name = "uniProtRdfRestTemplate")
    private RestTemplate restTemplate;

    @Autowired private TaxonomyLineageRepository taxRepository;
    @Autowired private EmbeddingsQueueConfigProperties embeddingsQueueConfigProperties;

    @BeforeAll
    void beforeAll() throws Exception {
        prepareDownloadFolders();
        UniProtKBAsyncDownloadUtils.saveEntriesInSolrAndStore(
                uniProtKBQueryRepository, cloudSolrClient, solrClient, storeClient, taxRepository);
    }

    @BeforeEach
    void setUp() {
        asyncDownloadFileHandler = uniProtKBAsyncDownloadFileHandler;
        downloadJobRepository = uniProtKBDownloadJobRepository;
        messageConsumer = uniProtKBMessageConsumer;
        downloadConfigProperties = uniProtKBDownloadConfigProperties;
        downloadRequest = new UniProtKBDownloadRequest();
        downloadRequest.setFormat("json");
        downloadRequest.setQuery("Human");
        downloadRequest.setFields("accession,id,gene_names");
        UniProtKBAsyncDownloadUtils.setUp(restTemplate);
        embeddingsQueueConfigProperties.setMaxEntryCount(MAX_ENTRY_COUNT);
    }

    @Test
    void onMessage_embeddingsAborted() {
        embeddingsQueueConfigProperties.setMaxEntryCount(10);
        MessageProperties messageHeader = new MessageProperties();
        messageHeader.setHeader(JOB_ID_HEADER, ID);
        downloadRequest.setFormat(HDF5_MEDIA_TYPE_VALUE);
        Message message = messageConverter.toMessage(downloadRequest, messageHeader);
        saveDownloadJob(ID, 0, NEW, 0, 0);

        messageConsumer.onMessage(message);

        UniProtKBDownloadJob job = downloadJobRepository.findById(ID).get();
        assertEquals(0, job.getRetried());
        assertEquals(0, job.getTotalEntries());
        assertFalse(asyncDownloadFileHandler.areAllFilesExist(ID));
        assertEquals(0, job.getUpdateCount());
        assertEquals(ABORTED, job.getStatus());
        assertEquals(0, job.getProcessedEntries());
    }

    @Test
    void onMessage_withTaxonomyFields() throws Exception {
        MessageProperties messageHeader = new MessageProperties();
        messageHeader.setHeader(JOB_ID_HEADER, ID);
        downloadRequest.setQuery("content:*");
        downloadRequest.setFields("accession,lineage");
        Message message = messageConverter.toMessage(downloadRequest, messageHeader);
        saveDownloadJob(ID, 0, NEW, 0, 0);

        messageConsumer.onMessage(message);

        UniProtKBDownloadJob job = downloadJobRepository.findById(ID).get();
        assertEquals(0, job.getRetried());
        assertEquals(12, job.getTotalEntries());
        assertFalse(asyncDownloadFileHandler.areAllFilesExist(ID));
        assertEquals(8, job.getUpdateCount());
        assertEquals(FINISHED, job.getStatus());
        assertEquals(12, job.getProcessedEntries());
        verifyIdsFiles(ID);
        Path resultFilePath =
                Path.of(
                        getTestAsyncConfig().getResultFolder()
                                + "/"
                                + ID
                                + FileType.GZIP.getExtension());
        assertTrue(Files.exists(resultFilePath));
        Path unzippedFile = Path.of(getTestAsyncConfig().getResultFolder() + "/" + ID);
        uncompressFile(resultFilePath, unzippedFile);
        assertTrue(Files.exists(unzippedFile));
        String resultsJson = Files.readString(unzippedFile);
        List<String> primaryAccessions = JsonPath.read(resultsJson, "$.results.*.primaryAccession");
        assertEquals(12, primaryAccessions.size());
        List<String> lineages = JsonPath.read(resultsJson, "$.results.*.lineages");
        assertFalse(lineages.isEmpty());
        assertEquals(12, lineages.size());
    }

    @Override
    protected TestAsyncConfig getTestAsyncConfig() {
        return uniProtKBAsyncConfig;
    }

    @Override
    protected List<SolrCollection> getSolrCollections() {
        return List.of(SolrCollection.uniprot, SolrCollection.taxonomy);
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
                                "P00001", "P00002", "P00003", "P00004", "P00005", "P00006",
                                "P00007", "P00008", "P00009", "P00010")));
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
        List<String> primaryAccessions = JsonPath.read(resultsJson, "$.results.*.primaryAccession");
        assertEquals(12, primaryAccessions.size());
        List<String> uniProtkbIds = JsonPath.read(resultsJson, "$.results.*.uniProtkbId");
        assertEquals(12, uniProtkbIds.size());
        List<String> genes = JsonPath.read(resultsJson, "$.results.*.genes");
        assertEquals(12, genes.size());
        List<String> comments = JsonPath.read(resultsJson, "$.results.*.comments");
        assertEquals(0, comments.size());
        List<String> organisms = JsonPath.read(resultsJson, "$.results.*.organism");
        assertEquals(0, organisms.size());
    }

    @Override
    protected void assertJobSpecifics(UniProtKBDownloadJob job, String format) {
        assertEquals(
                Objects.equals(format, LIST_MEDIA_TYPE_VALUE)
                        ? 12
                        : isRdfType(format) ? 7 : isH5Format(format) ? 6 : 8,
                job.getUpdateCount());
        assertEquals(isH5Format(format) ? UNFINISHED : FINISHED, job.getStatus());
        assertEquals(isH5Format(format) ? 0 : 12, job.getProcessedEntries());
    }

    private static boolean isH5Format(String format) {
        return Set.of(HDF5_MEDIA_TYPE_VALUE, "h5").contains(format);
    }

    @Override
    protected void saveDownloadJob(
            String id,
            int retryCount,
            JobStatus jobStatus,
            long updateCount,
            long processedEntries) {
        uniProtKBDownloadJobRepository.save(
                UniProtKBDownloadJob.builder()
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
                        N_TRIPLES_MEDIA_TYPE_VALUE,
                        HDF5_MEDIA_TYPE_VALUE,
                        "h5")
                .map(Arguments::of);
    }
}
