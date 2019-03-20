package uk.ac.ebi.uniprot.uniprotkb.repository;

import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import uk.ac.ebi.uniprot.common.repository.DataStoreManager;
import uk.ac.ebi.uniprot.common.repository.search.ClosableEmbeddedSolrClient;
import uk.ac.ebi.uniprot.common.repository.search.SolrCollection;
import uk.ac.ebi.uniprot.common.repository.search.SolrDataStoreManager;
import uk.ac.ebi.uniprot.common.repository.search.mockers.GoRelationsRepoMocker;
import uk.ac.ebi.uniprot.common.repository.search.mockers.TaxonomyRepoMocker;
import uk.ac.ebi.uniprot.dataservice.document.impl.InactiveEntryConverter;
import uk.ac.ebi.uniprot.dataservice.document.impl.UniprotEntryConverter;
import uk.ac.ebi.uniprot.dataservice.voldemort.VoldemortClient;
import uk.ac.ebi.uniprot.dataservice.voldemort.uniprot.VoldemortInMemoryUniprotEntryStore;
import uk.ac.ebi.uniprot.uniprotkb.repository.store.UniProtStoreClient;

import java.io.IOException;
import java.net.URISyntaxException;

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
        ClosableEmbeddedSolrClient solrClient = new ClosableEmbeddedSolrClient(SolrCollection.uniprot);
        addUniProtStoreInfo(dataStoreManager, solrClient);
        return solrClient;
    }

    @Bean
    @Profile("offline")
    public UniProtStoreClient primaryUniProtStoreClient(DataStoreManager dsm) {
        UniProtStoreClient storeClient = new UniProtStoreClient(VoldemortInMemoryUniprotEntryStore
                                                                               .getInstance("avro-uniprot"));
        dsm.addVoldemort(DataStoreManager.StoreType.UNIPROT, storeClient);
        return storeClient;
    }

    private void addUniProtStoreInfo(DataStoreManager dsm, ClosableEmbeddedSolrClient uniProtSolrClient) throws URISyntaxException {
        dsm.addDocConverter(DataStoreManager.StoreType.UNIPROT, new UniprotEntryConverter(TaxonomyRepoMocker.getTaxonomyRepo(), GoRelationsRepoMocker.getGoRelationRepo()));
        dsm.addDocConverter(DataStoreManager.StoreType.INACTIVE_UNIPROT, new InactiveEntryConverter());

        dsm.addSolrClient(DataStoreManager.StoreType.UNIPROT, uniProtSolrClient);
        dsm.addSolrClient(DataStoreManager.StoreType.INACTIVE_UNIPROT, uniProtSolrClient);
    }


}
