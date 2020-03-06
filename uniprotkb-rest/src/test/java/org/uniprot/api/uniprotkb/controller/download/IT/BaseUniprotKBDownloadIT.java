package org.uniprot.api.uniprotkb.controller.download.IT;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.BeforeAll;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.rest.controller.AbstractDownloadControllerIT;
import org.uniprot.api.uniprotkb.repository.search.impl.UniprotQueryRepository;
import org.uniprot.api.uniprotkb.repository.store.UniProtKBStoreClient;
import org.uniprot.core.builder.SequenceBuilder;
import org.uniprot.core.gene.Gene;
import org.uniprot.core.uniprot.UniProtEntry;
import org.uniprot.core.uniprot.UniProtEntryType;
import org.uniprot.core.uniprot.builder.GeneBuilder;
import org.uniprot.core.uniprot.builder.GeneNameBuilder;
import org.uniprot.core.uniprot.builder.UniProtEntryBuilder;
import org.uniprot.core.uniprot.taxonomy.Organism;
import org.uniprot.core.uniprot.taxonomy.builder.OrganismBuilder;
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
    public static final String MNEMONIC1 = "MNEMONIC_A";
    public static final String MNEMONIC2 = "MNEMONIC_B";
    public static final String MNEMONIC3 = "MNEMONIC_C";

    public static List<String> TSV_DEFAULT_FIELDS = Arrays.asList("Entry","Entry Name","Reviewed","Protein names","Gene Names","Organism","Length");
    public static List<String> SORTED_BY_LENGTH = Arrays.asList(ACC3, ACC2, ACC1);
    public static List<String> SORTED_BY_MASS_DESC = Arrays.asList(ACC1, ACC2, ACC3);
    public static List<String> SORTED_BY_ACCESSION = Arrays.asList(ACC1, ACC2, ACC3);
    public static List<String> SORTED_BY_ACCESSION_DESC = Arrays.asList(ACC3, ACC2, ACC1);
    public static List<String> SORTED_BY_ANNOTATION_SCORE = Arrays.asList(ACC2, ACC3, ACC1);
    public static List<String> SORTED_BY_MNEMONIC = Arrays.asList(ACC2, ACC1, ACC3);
    public static List<String> SORTED_BY_GENE = Arrays.asList(ACC2, ACC3, ACC1);
    public static List<String> SORTED_BY_ORGANISM = Arrays.asList(ACC2, ACC3, ACC1);
    public static List<String> MANDATORY_JSON_FIELDS =
            Arrays.asList(
                    "entryType", "primaryAccession", "uniProtId", "entryAudit", "annotationScore");
    public static List<String> REQUESTED_JSON_FIELDS =
            Arrays.asList("protein_existence", "organism", "protein_name");
    public static List<String> INVALID_RETURN_FIELDS =
            Arrays.asList("protein", "accession", "kinetic");
    public static List<String> RETURNED_JSON_FIELDS =
            new ArrayList<>(Arrays.asList("proteinExistence", "organism", "proteinDescription"));

    static {
        RETURNED_JSON_FIELDS.addAll(MANDATORY_JSON_FIELDS);
    }

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
        Gene gene = entry.getGenes().get(0);
        if (ACC1.equals(accession)) {
            builder.organism(getOrganism("root", 1));
            builder.genesAdd(getGene(gene, "cUrl"));
            builder.uniProtId(MNEMONIC2);
            builder.annotationScore(5.0);
            builder.sequence(new SequenceBuilder(SEQUENCE1).build());
        } else if (ACC2.equals(accession)) {
            builder.organism(getOrganism("Bacteria", 2));
            builder.genesAdd(getGene(gene, "aUrl"));
            builder.annotationScore(1.0);
            builder.uniProtId(MNEMONIC1);
            builder.sequence(new SequenceBuilder(SEQUENCE2).build());
        } else if (ACC3.equals(accession)) {
            builder.organism(getOrganism("Human", 9606l));
            builder.genesAdd(getGene(gene, "bUrl"));
            builder.uniProtId(MNEMONIC3);
            builder.annotationScore(3.0);
            builder.sequence(new SequenceBuilder(SEQUENCE3).build());
        }

        builder.entryType(UniProtEntryType.SWISSPROT);

        this.getStoreManager().save(DataStoreManager.StoreType.UNIPROT, builder.build());
    }

    private Gene getGene(Gene oldGene, String name) {
        GeneBuilder newGene =
                GeneBuilder.from(oldGene)
                        .geneName(
                                new GeneNameBuilder(name, oldGene.getGeneName().getEvidences())
                                        .build());
        return newGene.build();
    }

    private Organism getOrganism(String name, long taxonId) {
        return new OrganismBuilder().commonName(name).taxonId(taxonId).build();
    }
}
