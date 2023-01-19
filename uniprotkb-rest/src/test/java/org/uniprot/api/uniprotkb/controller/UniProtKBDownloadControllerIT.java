package org.uniprot.api.uniprotkb.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.common.repository.stream.store.uniprotkb.TaxonomyLineageRepository;
import org.uniprot.api.rest.controller.AbstractStreamControllerIT;
import org.uniprot.api.rest.download.MessageQueueTestConfig;
import org.uniprot.api.rest.download.message.EmbeddedInMemoryQpidBroker;
import org.uniprot.api.rest.download.repository.CommonRestTestConfig;
import org.uniprot.api.rest.download.repository.DownloadJobRepository;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.api.uniprotkb.UniProtKBREST;
import org.uniprot.api.uniprotkb.repository.DataStoreTestConfig;
import org.uniprot.core.json.parser.taxonomy.TaxonomyJsonConfig;
import org.uniprot.core.taxonomy.TaxonomyEntry;
import org.uniprot.core.taxonomy.TaxonomyRank;
import org.uniprot.core.taxonomy.impl.TaxonomyEntryBuilder;
import org.uniprot.core.taxonomy.impl.TaxonomyLineageBuilder;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.uniprotkb.impl.UniProtKBEntryBuilder;
import org.uniprot.cv.chebi.ChebiRepo;
import org.uniprot.cv.ec.ECRepo;
import org.uniprot.cv.go.GORepo;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.indexer.uniprot.mockers.PathwayRepoMocker;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.indexer.uniprot.mockers.UniProtEntryMocker;
import org.uniprot.store.indexer.uniprotkb.converter.UniProtEntryConverter;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.taxonomy.TaxonomyDocument;
import org.uniprot.store.search.document.uniprot.UniProtDocument;

@Slf4j
@ActiveProfiles(profiles = "offline")
@WebMvcTest({UniProtKBDownloadController.class})
@ContextConfiguration(
        classes = {
            DataStoreTestConfig.class,
            UniProtKBREST.class,
            ErrorHandlerConfig.class,
            MessageQueueTestConfig.class,
            CommonRestTestConfig.class
        })
@ExtendWith(SpringExtension.class)
@AutoConfigureWebClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UniProtKBDownloadControllerIT extends AbstractStreamControllerIT {
// TODO currently we manually need to create test ids and result folders
// download.idFilesFolder=target/download/ids
// download.resultFilesFolder=target/download/result
// ideally they  should be created on  start of application if not already created
    private static final String DOWNLOAD_RUN_PATH = UniProtKBDownloadController.DOWNLOAD_RESOURCE + "/run";
    private static final UniProtKBEntry TEMPLATE_ENTRY =
            UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
    private final UniProtEntryConverter documentConverter =
            new UniProtEntryConverter(
                    TaxonomyRepoMocker.getTaxonomyRepo(),
                    Mockito.mock(GORepo.class),
                    PathwayRepoMocker.getPathwayRepo(),
                    mock(ChebiRepo.class),
                    mock(ECRepo.class),
                    new HashMap<>());
    @Autowired UniProtStoreClient<UniProtKBEntry> storeClient;
    @Autowired private MockMvc mockMvc;
    @Autowired
    private EmbeddedInMemoryQpidBroker embeddedBroker;
    @Autowired
    @Qualifier("uniProtKBSolrClient")
    private SolrClient solrClient;

    @Autowired private FacetTupleStreamTemplate facetTupleStreamTemplate;
    @Autowired private TupleStreamTemplate tupleStreamTemplate;
    @Autowired private TaxonomyLineageRepository taxRepository;

    @Autowired private RabbitTemplate rabbitTemplate; // RabbitTemplate with inmemory qpid broker

    @Autowired
    private DownloadJobRepository
            downloadJobRepository; // RedisRepository with inMemory redis server


    @Value("${download.idFilesFolder}")
    private String idsFolder;
    @Value("${download.resultFilesFolder}")
    private String resultFolder;


    @BeforeAll
    void saveEntriesInSolrAndStore() throws Exception {
//        startEmbeddedMessageBroker();
        saveEntries();

        // for the following tests, ensure the number of hits
        // for each query is less than the maximum number allowed
        // to be streamed (configured in {@link
        // org.uniprot.api.common.repository.store.StreamerConfigProperties})
        long queryHits = 100L;
        QueryResponse response = mock(QueryResponse.class);
        SolrDocumentList results = mock(SolrDocumentList.class);
        when(results.getNumFound()).thenReturn(queryHits);
        when(response.getResults()).thenReturn(results);
        when(solrClient.query(anyString(), any())).thenReturn(response);

        ReflectionTestUtils.setField(taxRepository, "solrClient", cloudSolrClient);
    }

    @Test
    void sendAndProcessDownloadMessage() throws Exception {
        // producer sends the download request to the queue
        // producer uses downloadJobRepository to write to in memory
        // consumer receives the message
        // writes to ids and result
        Assertions.assertNotNull("true");
        System.out.println("stub");
        // when

        MockHttpServletRequestBuilder requestBuilder =
                get(DOWNLOAD_RUN_PATH)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "content:*");
        ResultActions response = this.mockMvc.perform(requestBuilder);

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.*",
                                Matchers.notNullValue()));
        String jobId = response.andReturn().getResponse().getContentAsString();
        Thread.sleep(10000); // TODO give enough time to consumer to process the request.. ideally we should loop on status api call until the job is completed
        // verify the ids file and clean up
        Path idsFilePath = Path.of(this.idsFolder + "/" + jobId);
        Assertions.assertTrue(Files.exists(idsFilePath));
        List<String> ids = Files.readAllLines(idsFilePath);
        Assertions.assertNotNull(ids);
        Assertions.assertTrue(ids.containsAll(List.of("P00001", "P00002", "P00003", "P00004", "P00005", "P00006",
                "P00007", "P00008", "P00009", "P00010")));
        Files.delete(idsFilePath);
        Assertions.assertTrue(Files.notExists(idsFilePath));
        // verify result file and delete it
        Path resultFilePath = Path.of(this.resultFolder + "/" + jobId);
        Assertions.assertTrue(Files.exists(resultFilePath));
        String resultsJson = Files.readString(resultFilePath);
        List<String> primaryAccessions = JsonPath.read(resultsJson, "$.results.*.primaryAccession");
        Assertions.assertTrue(List.of("P00001", "P00002", "P00003", "P00004", "P00005", "P00006",
                        "P00007", "P00008", "P00009", "P00010").containsAll(primaryAccessions));
        Files.delete(resultFilePath);
        Assertions.assertTrue(Files.notExists(resultFilePath));
    }

    @Override
    protected List<SolrCollection> getSolrCollections() {
        return List.of(SolrCollection.uniprot, SolrCollection.taxonomy);
    }

    @Override
    protected TupleStreamTemplate getTupleStreamTemplate() {
        return this.tupleStreamTemplate;
    }

    @Override
    protected FacetTupleStreamTemplate getFacetTupleStreamTemplate() {
        return this.facetTupleStreamTemplate;
    }

    private void saveEntries() throws Exception {
        for (int i = 1; i <= 10; i++) {
            saveEntry(i, "");
        }
        saveEntry(11, "-2");
        saveEntry(12, "-2");
        cloudSolrClient.commit(SolrCollection.uniprot.name());

        saveTaxonomyEntry(9606L);
        cloudSolrClient.commit(SolrCollection.taxonomy.name());
    }

    private void saveTaxonomyEntry(long taxId) throws Exception {
        TaxonomyEntryBuilder entryBuilder = new TaxonomyEntryBuilder();
        TaxonomyEntry taxonomyEntry =
                entryBuilder
                        .taxonId(taxId)
                        .rank(TaxonomyRank.SPECIES)
                        .lineagesAdd(new TaxonomyLineageBuilder().taxonId(taxId + 1).build())
                        .lineagesAdd(new TaxonomyLineageBuilder().taxonId(taxId + 2).build())
                        .build();
        byte[] taxonomyObj =
                TaxonomyJsonConfig.getInstance()
                        .getFullObjectMapper()
                        .writeValueAsBytes(taxonomyEntry);

        TaxonomyDocument.TaxonomyDocumentBuilder docBuilder =
                TaxonomyDocument.builder()
                        .taxId(taxId)
                        .id(String.valueOf(taxId))
                        .taxonomyObj(taxonomyObj);
        cloudSolrClient.addBean(SolrCollection.taxonomy.name(), docBuilder.build());
    }

    private void saveEntry(int i, String isoFormString) throws Exception {
        UniProtKBEntryBuilder entryBuilder = UniProtKBEntryBuilder.from(TEMPLATE_ENTRY);
        String acc = String.format("P%05d", i) + isoFormString;
        entryBuilder.primaryAccession(acc);

        UniProtKBEntry uniProtKBEntry = entryBuilder.build();
        UniProtDocument convert = documentConverter.convert(uniProtKBEntry);

        cloudSolrClient.addBean(SolrCollection.uniprot.name(), convert);
        storeClient.saveEntry(uniProtKBEntry);
    }
}
