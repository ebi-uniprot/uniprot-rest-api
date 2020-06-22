package org.uniprot.api.uniprotkb.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.uniprot.api.uniprotkb.UniProtKBREST;
import org.uniprot.api.uniprotkb.repository.DataStoreTestConfig;
import org.uniprot.api.uniprotkb.repository.search.impl.LiteratureRepository;
import org.uniprot.api.uniprotkb.repository.store.UniProtKBStoreClient;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;

/**
 * @author lgonzales
 * @since 2019-07-10
 */
@Slf4j
@ContextConfiguration(classes = {DataStoreTestConfig.class, UniProtKBREST.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(UniProtKBEntryController.class)
@AutoConfigureWebClient
@ExtendWith(value = {SpringExtension.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UniProtKBGetByAccessionsIT {

    @Autowired private LiteratureRepository repository;
    @Autowired UniProtKBEntryController uniProtKBEntryController;

    @Autowired private UniProtKBStoreClient storeClient;
    @Autowired private WebApplicationContext webApplicationContext;

    @Autowired private MockMvc mockMvc;

    @RegisterExtension static DataStoreManager storeManager = new DataStoreManager();

    @BeforeAll
    void initSolrAndInjectItInTheRepository() {
        storeManager.addSolrClient(
                DataStoreManager.StoreType.LITERATURE, SolrCollection.literature);
        storeManager.addStore(DataStoreManager.StoreType.UNIPROT, storeClient);
        ReflectionTestUtils.setField(
                repository,
                "solrClient",
                storeManager.getSolrClient(DataStoreManager.StoreType.LITERATURE));
    }

    @BeforeEach
    void cleanData() {
        storeManager.cleanSolr(DataStoreManager.StoreType.LITERATURE);
        storeManager.cleanStore(DataStoreManager.StoreType.UNIPROT);
    }

    @Test
    void getProteinsByAccessions() throws Exception {
        // given
        long gStart = System.currentTimeMillis();
        int userCount = 1;
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        List<Callable<MvcResult>> tasks = new ArrayList<>();
        final AtomicInteger atmInt = new AtomicInteger(0);
        for (int i = 0; i < userCount; i++) {
            Callable<MvcResult> task =
                    () -> {
                        String dataFile =
                                "/Users/sahmad/Documents/accessions"
                                        + atmInt.getAndIncrement() % 10
                                        + ".txt";
                        log.info("Data file is " + dataFile);
                        long start = System.currentTimeMillis();
                        int count = 1000;
                        List<String> lines = Files.readAllLines(Paths.get(dataFile));
                        Collections.shuffle(lines);
                        String accessions =
                                lines.subList(0, count).stream().collect(Collectors.joining(","));
                        MockMvc mockMvc1 =
                                MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
                        MvcResult result =
                                mockMvc1.perform(
                                                get("/uniprotkb/proteins")
                                                        .param("accessions", accessions)
                                                        .header(ACCEPT, APPLICATION_JSON_VALUE))
                                        .andReturn();
                        long end = System.currentTimeMillis();
                        log.info(
                                "Total time taken: "
                                        + (end - start)
                                        + " milliseconds"
                                        + " for the file: "
                                        + dataFile);
                        return result;
                    };
            tasks.add(task);
        }
        List<Future<MvcResult>> futures = executorService.invokeAll(tasks);
        int failedCount = 0;
        for (Future<MvcResult> future : futures) {
            MvcResult mvcResult = future.get();
            log.info(mvcResult.getResponse().getContentAsString());
            if (mvcResult.getResponse().getStatus() != 200) {
                failedCount++;
            }
        }
        Assertions.assertEquals(0, failedCount, "No. of failed requests");
        awaitTerminationAfterShutdown(executorService);
        log.info(
                "Grant total time taken: "
                        + (System.currentTimeMillis() - gStart)
                        + " milliseconds");
    }

    public void awaitTerminationAfterShutdown(ExecutorService threadPool) {
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(60, TimeUnit.MINUTES)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException ex) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
