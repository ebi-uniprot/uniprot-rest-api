package org.uniprot.api.uniprotkb.repository;

import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.core.CoreContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.api.uniprotkb.repository.store.UniProtKBStoreClient;

import org.uniprot.core.cv.chebi.ChebiRepo;
import org.uniprot.core.cv.ec.ECRepo;
import org.uniprot.store.datastore.voldemort.VoldemortClient;
import org.uniprot.store.datastore.voldemort.uniprot.VoldemortInMemoryUniprotEntryStore;
import org.uniprot.store.indexer.ClosableEmbeddedSolrClient;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.indexer.SolrDataStoreManager;
import org.uniprot.store.indexer.uniprot.mockers.GoRelationsRepoMocker;
import org.uniprot.store.indexer.uniprot.mockers.PathwayRepoMocker;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.indexer.uniprotkb.processor.InactiveEntryConverter;
import org.uniprot.store.indexer.uniprotkb.processor.UniProtEntryConverter;
import org.uniprot.store.search.SolrCollection;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;

import static org.mockito.Mockito.mock;


/**
 * A test configuration providing {@link SolrClient} and {@link VoldemortClient} beans that override production ones.
 * For example, this allows us to use embedded Solr data stores or in memory Voldemort instances, rather than ones
 * running on VMs.
 * <p>
 * Created 14/09/18
 *
 * @author Edd
 */
@TestConfiguration
public class DataStoreTestConfig {
    @Bean(destroyMethod = "close")
    public DataStoreManager dataStoreManager() throws IOException {
        SolrDataStoreManager sdsm = new SolrDataStoreManager();
        return new DataStoreManager(sdsm);
    }

    @Bean
    @Profile("offline")
    public HttpClient httpClient() {
        return mock(HttpClient.class);
    }

    @Bean
    @Profile("offline")
    public SolrClient uniProtSolrClient(DataStoreManager dataStoreManager) throws URISyntaxException {
        CoreContainer container = new CoreContainer(new File(System.getProperty(ClosableEmbeddedSolrClient.SOLR_HOME)).getAbsolutePath());
        container.load();
        ClosableEmbeddedSolrClient solrClient = new ClosableEmbeddedSolrClient(container, SolrCollection.uniprot);
        addUniProtStoreInfo(dataStoreManager, solrClient);
        return solrClient;
    }

    @SuppressWarnings("rawtypes")
	@Bean
    @Profile("offline")
    public UniProtKBStoreClient primaryUniProtStoreClient(DataStoreManager dsm) {
        UniProtKBStoreClient storeClient = new UniProtKBStoreClient(VoldemortInMemoryUniprotEntryStore
                                                                        .getInstance("avro-uniprot"));
        dsm.addStore(DataStoreManager.StoreType.UNIPROT, storeClient);
        return storeClient;
    }

    @Bean
    @Profile("offline")
    public SolrRequestConverter requestConverter() {
        return new SolrRequestConverter() {
            @Override
            public SolrQuery toSolrQuery(SolrRequest request) {
                SolrQuery solrQuery = super.toSolrQuery(request);

                // required for tests, because EmbeddedSolrServer is not sharded
                solrQuery.setParam("distrib", "false");
                solrQuery.setParam("terms.mincount", "1");

                return solrQuery;
            }
        };
    }

    private void addUniProtStoreInfo(DataStoreManager dsm, ClosableEmbeddedSolrClient uniProtSolrClient) throws URISyntaxException {
        dsm.addDocConverter(DataStoreManager.StoreType.UNIPROT,
                            new UniProtEntryConverter(TaxonomyRepoMocker.getTaxonomyRepo(),
                                                      GoRelationsRepoMocker.getGoRelationRepo(),
                                                      PathwayRepoMocker.getPathwayRepo(),
                                                      mock(ChebiRepo.class),
                                                      mock(ECRepo.class),
                                                      new HashMap<>()));
        dsm.addDocConverter(DataStoreManager.StoreType.INACTIVE_UNIPROT, new InactiveEntryConverter());

        dsm.addSolrClient(DataStoreManager.StoreType.UNIPROT, uniProtSolrClient);
        dsm.addSolrClient(DataStoreManager.StoreType.INACTIVE_UNIPROT, uniProtSolrClient);
    }
}
