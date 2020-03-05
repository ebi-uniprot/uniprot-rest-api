package org.uniprot.api.uniprotkb.controller.download.IT;

import org.junit.jupiter.api.BeforeAll;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.rest.controller.AbstractDownloadControllerIT;
import org.uniprot.api.uniprotkb.repository.search.impl.UniprotQueryRepository;
import org.uniprot.api.uniprotkb.repository.store.UniProtKBStoreClient;
import org.uniprot.core.builder.SequenceBuilder;
import org.uniprot.core.uniprot.UniProtEntry;
import org.uniprot.core.uniprot.UniProtEntryType;
import org.uniprot.core.uniprot.builder.UniProtEntryBuilder;
import org.uniprot.cv.chebi.ChebiRepo;
import org.uniprot.cv.ec.ECRepo;
import org.uniprot.store.datastore.voldemort.uniprot.VoldemortInMemoryUniprotEntryStore;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.indexer.uniprot.mockers.GoRelationsRepoMocker;
import org.uniprot.store.indexer.uniprot.mockers.PathwayRepoMocker;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.indexer.uniprot.mockers.UniProtEntryMocker;
import org.uniprot.store.indexer.uniprotkb.converter.UniProtEntryConverter;
import org.uniprot.store.indexer.uniprotkb.processor.InactiveEntryConverter;
import org.uniprot.store.search.SolrCollection;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Class to keep things common to all disease download tests */
public class BaseUniprotKBDownloadIT extends AbstractDownloadControllerIT {
    @Autowired private UniprotQueryRepository repository;
    public static String SEARCH_ACCESSION1 =
            "O-" + ThreadLocalRandom.current().nextLong(10000, 50000);
    public static String SEARCH_ACCESSION2 =
            "P-" + ThreadLocalRandom.current().nextLong(50001, 99999);


    public static final String ACC1 = "O12345";
    public static final String ACC2 = "P12345";
    public static final String ACC3 = "Q12345";
    public static final String SEQUENCE1 = "KDVHMPKHPELADKNVPNLHVMKAMQS";
    public static final String SEQUENCE2 = "RIAIHELLFKEGVMVAK";
    public static final String SEQUENCE3 = "MLMPKKN";

    public static List<String> SORTED_BY_LENGTH = Arrays.asList(ACC3, ACC2, ACC1);

    private UniProtKBStoreClient storeClient;

    @BeforeAll
    void setUp() {
        UniProtEntryConverter uniProtEntryConverter =
                new UniProtEntryConverter(
                        TaxonomyRepoMocker.getTaxonomyRepo(),
                        GoRelationsRepoMocker.getGoRelationRepo(),
                        PathwayRepoMocker.getPathwayRepo(),
                        Mockito.mock(ChebiRepo.class),
                        Mockito.mock(ECRepo.class),
                        new HashMap<>());

        DataStoreManager dsm = getStoreManager();
        dsm.addDocConverter(DataStoreManager.StoreType.UNIPROT, uniProtEntryConverter);
        dsm.addDocConverter(
                DataStoreManager.StoreType.INACTIVE_UNIPROT, new InactiveEntryConverter());
        dsm.addSolrClient(DataStoreManager.StoreType.INACTIVE_UNIPROT, SolrCollection.uniprot);

        this.storeClient =
                new UniProtKBStoreClient(
                        VoldemortInMemoryUniprotEntryStore.getInstance("avro-uniprot"));
        dsm.addStore(DataStoreManager.StoreType.UNIPROT, storeClient);
    }

    @Override
    protected DataStoreManager.StoreType getStoreType() {
        return DataStoreManager.StoreType.UNIPROT;
    }

    @Override
    protected SolrCollection getSolrCollection() {
        return SolrCollection.uniprot;
    }

    @Override
    protected SolrQueryRepository getRepository() {
        return repository;
    }

    @Override
    protected String getDownloadRequestPath() {
        return "/uniprotkb/download";
    }

    @Override
    protected void saveEntries(int numberOfEntries) {
        List<UniProtEntry> entries =
                UniProtEntryMocker.cloneEntries(UniProtEntryMocker.Type.SP, numberOfEntries);
        this.getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entries);
    }

    @Override
    protected void saveEntry(String accession, long suffix) {
        UniProtEntry entry = UniProtEntryMocker.create(accession);
        UniProtEntryBuilder builder = UniProtEntryBuilder.from(entry);
        if(ACC1.equals(accession)){
            builder.sequence(new SequenceBuilder(SEQUENCE1).build());
        } else if(ACC2.equals(accession)){
            builder.sequence(new SequenceBuilder(SEQUENCE2).build());
        } else if(ACC3.equals(accession)){
            builder.sequence(new SequenceBuilder(SEQUENCE3).build());
        }

        builder.entryType(UniProtEntryType.SWISSPROT);

        this.getStoreManager().save(DataStoreManager.StoreType.UNIPROT, builder.build());
    }
}
