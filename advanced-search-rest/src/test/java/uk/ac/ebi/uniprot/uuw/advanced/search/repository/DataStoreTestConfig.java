package uk.ac.ebi.uniprot.uuw.advanced.search.repository;

import org.apache.solr.client.solrj.SolrClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import uk.ac.ebi.uniprot.dataservice.document.impl.UniprotEntryConverter;
import uk.ac.ebi.uniprot.dataservice.serializer.avro.EntryConverter;
import uk.ac.ebi.uniprot.dataservice.source.impl.go.GoRelationFileReader;
import uk.ac.ebi.uniprot.dataservice.source.impl.go.GoRelationFileRepo;
import uk.ac.ebi.uniprot.dataservice.source.impl.go.GoRelationRepo;
import uk.ac.ebi.uniprot.dataservice.source.impl.go.GoTermFileReader;
import uk.ac.ebi.uniprot.dataservice.source.impl.taxonomy.FileNodeIterable;
import uk.ac.ebi.uniprot.dataservice.source.impl.taxonomy.TaxonomyMapRepo;
import uk.ac.ebi.uniprot.dataservice.source.impl.taxonomy.TaxonomyRepo;
import uk.ac.ebi.uniprot.dataservice.voldemort.VoldemortClient;
import uk.ac.ebi.uniprot.dataservice.voldemort.uniprot.VoldemortInMemoryUniprotEntryStore;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * A test configuration providing {@link SolrClient} and {@link VoldemortClient} beans that override production ones.
 * For example, this allows us to use embedded Solr data stores or in memory Voldemort instances, rather than ones
 * running on VMs.
 * <p>
 * Created 14/09/18
 *
 * @author Edd
 */
@Configuration
public class DataStoreTestConfig {
    @Bean(destroyMethod = "close")
    public DataStoreManager dataStoreManager() throws IOException {
        SolrDataStoreManager sdsm = new SolrDataStoreManager();
        return new DataStoreManager(sdsm);
    }

    @Bean
    @Primary
    public SolrClient uniProtSolrClient(DataStoreManager dataStoreManager) throws IOException, URISyntaxException {
        ClosableEmbeddedSolrClient solrClient = new ClosableEmbeddedSolrClient(SolrCollection.uniprot);
        addUniProtStoreInfo(dataStoreManager, solrClient);
        return solrClient;
    }

    @Bean
    @Profile("hello")
    @Primary
    public VoldemortClient uniProtClient(DataStoreManager dsm) {
        VoldemortInMemoryUniprotEntryStore entryStore = VoldemortInMemoryUniprotEntryStore.getInstance("avro-uniprot");
        dsm.addVoldemort(DataStoreManager.StoreType.UNIPROT, entryStore);
        dsm.addEntryConverter(DataStoreManager.StoreType.UNIPROT, new EntryConverter());
        return entryStore;
    }

    private void addUniProtStoreInfo(DataStoreManager dsm, ClosableEmbeddedSolrClient uniProtSolrClient) throws URISyntaxException {
        dsm.addDocConverter(DataStoreManager.StoreType.UNIPROT, new UniprotEntryConverter(taxonomyRepo(), goRelationRepo()));
        dsm.addSolrClient(DataStoreManager.StoreType.UNIPROT, uniProtSolrClient);
    }

    private File getTaxonomyFile() throws URISyntaxException {
        URL url = ClassLoader.getSystemClassLoader().getResource("taxonomy/taxonomy.dat");
        return new File(url.toURI());
    }

    private GoRelationRepo goRelationRepo() {
        String gotermPath = ClassLoader.getSystemClassLoader().getResource("goterm").getFile();
        return GoRelationFileRepo.create(new GoRelationFileReader(gotermPath),
                                                              new GoTermFileReader(gotermPath));
    }

    private TaxonomyRepo taxonomyRepo() throws URISyntaxException {
        File taxonomicFile = getTaxonomyFile();

        FileNodeIterable taxonomicNodeIterable = new FileNodeIterable(taxonomicFile);
        return new TaxonomyMapRepo(taxonomicNodeIterable);
    }
}
