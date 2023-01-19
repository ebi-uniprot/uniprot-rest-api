package org.uniprot.api.uniprotkb.queue;

import java.util.List;

import org.apache.solr.client.solrj.SolrClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.common.repository.stream.store.uniprotkb.TaxonomyLineageService;
import org.uniprot.api.rest.controller.AbstractStreamControllerIT;
import org.uniprot.api.rest.download.MessageQueueTestConfig;
import org.uniprot.api.rest.download.repository.CommonRestTestConfig;
import org.uniprot.api.rest.download.repository.DownloadJobRepository;
import org.uniprot.api.uniprotkb.repository.DataStoreTestConfig;
import org.uniprot.api.uniprotkb.repository.store.ResultsConfig;
import org.uniprot.api.uniprotkb.repository.store.UniProtStoreConfig;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.search.SolrCollection;

@ActiveProfiles(profiles = "offline")
@EnableConfigurationProperties
@PropertySource("classpath:application.properties")
@ContextConfiguration(
        classes = {
            DataStoreTestConfig.class,
            ResultsConfig.class,
            UniProtStoreConfig.class,
            MessageQueueTestConfig.class,
            CommonRestTestConfig.class
        })
@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AsyncDownloadIntegTest extends AbstractStreamControllerIT {
    // create solr client to index data
    // create voldemort client to index data
    // rabbittemplate to send message
    // redisjobrepository
    @Autowired private TupleStreamTemplate tupleStreamTemplate;
    @Autowired private FacetTupleStreamTemplate facetTupleStreamTemplate;

    @Autowired
    private UniProtStoreClient<UniProtKBEntry> storeClient; // in memory voldemort store client

    @Autowired
    @Qualifier("uniProtKBSolrClient")
    private SolrClient
            solrClient; // this is NOT inmemory solr cluster client, see CloudSolrClient in parent
    // class

    @Autowired private RabbitTemplate rabbitTemplate; // RabbitTemplate with inmemory qpid broker

    @Autowired
    private DownloadJobRepository
            downloadJobRepository; // RedisRepository with inMemory redis server

    @Autowired private TaxonomyLineageService taxonomyLineageService;
    @Autowired private RequestMappingHandlerMapping requestMappingHandlerMapping;

//    @Test
    void sendAndProcessDownloadMessage() {
        // producer sends the download request to the queue
        // producer uses downloadJobRepository to write to in memory
        // consumer receives the message
        // writes to ids and result
        System.out.println("stub");
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
}
