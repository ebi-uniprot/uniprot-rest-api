package org.uniprot.api.async.download.messaging.consumer.map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.async.download.AsyncDownloadRestApp;
import org.uniprot.api.async.download.common.UniRefAsyncDownloadUtils;
import org.uniprot.api.async.download.messaging.config.common.RedisConfiguration;
import org.uniprot.api.async.download.model.request.map.UniProtKBToUniRefMapDownloadRequest;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.common.repository.stream.store.uniprotkb.TaxonomyLineageRepository;
import org.uniprot.api.idmapping.common.service.TestConfig;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.api.uniprotkb.common.repository.UniProtKBDataStoreTestConfig;
import org.uniprot.api.uniprotkb.common.repository.search.UniprotQueryRepository;
import org.uniprot.api.uniprotkb.common.utils.UniProtKBAsyncDownloadUtils;
import org.uniprot.api.uniref.common.repository.UniRefDataStoreTestConfig;
import org.uniprot.api.uniref.common.repository.search.UniRefQueryRepository;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.search.SolrCollection;

import com.jayway.jsonpath.JsonPath;

@ActiveProfiles(profiles = {"offline", "idmapping"})
@ContextConfiguration(
        classes = {
            TestConfig.class,
            UniRefDataStoreTestConfig.class,
            UniProtKBDataStoreTestConfig.class,
            AsyncDownloadRestApp.class,
            ErrorHandlerConfig.class,
            RedisConfiguration.class
        })
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UniProtKBToUniRefMapMessageConsumerIT
        extends MapMessageConsumerIT<UniProtKBToUniRefMapDownloadRequest> {
    @Autowired private UniRefQueryRepository uniRefQueryRepository;
    @Autowired private UniprotQueryRepository uniprotQueryRepository;
    @Autowired private TaxonomyLineageRepository taxRepository;

    @Autowired
    @Qualifier("uniRefLightStoreClient")
    private UniProtStoreClient<UniRefEntryLight> uniRefStoreClient;

    @Qualifier("uniProtStoreClient")
    @Autowired
    private UniProtStoreClient<UniProtKBEntry> uniProtKBSolrClient;

    @Qualifier("uniRefFacetTupleStreamTemplate")
    @Autowired
    private FacetTupleStreamTemplate uniRefFacetTupleStreamTemplate;

    @Qualifier("uniProtKBFacetTupleStreamTemplate")
    @Autowired
    private FacetTupleStreamTemplate uniProtKBFacetTupleStreamTemplate;

    @Autowired
    @Qualifier("uniProtKBTupleStream")
    private TupleStreamTemplate uniProtKBTupleStream;

    @Autowired
    @Qualifier("uniRefTupleStreamTemplate")
    private TupleStreamTemplate uniRefTupleStreamTemplate;

    @MockBean(name = "uniRefRdfRestTemplate")
    private RestTemplate restTemplate;

    @BeforeAll
    void beforeAll() throws Exception {
        startClusterForMapping(uniRefTupleStreamTemplate, uniRefFacetTupleStreamTemplate);
        initBeforeAll();
        UniProtKBAsyncDownloadUtils.saveEntriesInSolrAndStore(
                uniprotQueryRepository,
                cloudSolrClient,
                solrClient,
                uniProtKBSolrClient,
                taxRepository);
        UniRefAsyncDownloadUtils.saveEntriesInSolrAndStoreForMapping(
                uniRefQueryRepository,
                cloudSolrClient,
                solrClient,
                uniRefStoreClient,
                new String[] {"", "P00001", "P00005", " P00007", "P00010"});
    }

    @BeforeEach
    void setUp() {
        initBeforeEach();
        downloadRequest = new UniProtKBToUniRefMapDownloadRequest();
        downloadRequest.setFormat("json");
        downloadRequest.setQuery("Human");
        downloadRequest.setFields("id,name,organism");
        downloadRequest.setTo("UniRef");
        UniRefAsyncDownloadUtils.setUp(restTemplate);
    }

    @Override
    protected List<SolrCollection> getSolrCollections() {
        return List.of(SolrCollection.uniref, SolrCollection.taxonomy, SolrCollection.uniprot);
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
                                "UniRef100_P03901",
                                "UniRef100_P03902",
                                "UniRef100_P03903",
                                "UniRef100_P03904",
                                "UniRef50_P03901",
                                "UniRef50_P03902",
                                "UniRef50_P03903",
                                "UniRef50_P03904",
                                "UniRef90_P03901",
                                "UniRef90_P03902",
                                "UniRef90_P03903",
                                "UniRef90_P03904")));
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
        List<String> ids = JsonPath.read(resultsJson, "$.results.*.id");
        assertEquals(12, ids.size());
        List<String> uniRefIds = JsonPath.read(resultsJson, "$.results.*.name");
        assertEquals(12, uniRefIds.size());
        List<String> representativeMember =
                JsonPath.read(resultsJson, "$.results.*.representativeMember");
        assertEquals(12, representativeMember.size());
        List<String> members = JsonPath.read(resultsJson, "$.results.*.members");
        assertEquals(12, members.size());
        List<String> organisms = JsonPath.read(resultsJson, "$.results.*.organisms");
        assertEquals(12, organisms.size());
    }

    @Override
    protected TupleStreamTemplate getTupleStreamTemplate() {
        return uniProtKBTupleStream;
    }

    @Override
    protected FacetTupleStreamTemplate getFacetTupleStreamTemplate() {
        return uniProtKBFacetTupleStreamTemplate;
    }
}
