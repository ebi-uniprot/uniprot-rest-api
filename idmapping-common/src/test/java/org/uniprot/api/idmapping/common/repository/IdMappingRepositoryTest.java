package org.uniprot.api.idmapping.common.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.util.MockSearchableSolrClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.uniprot.api.idmapping.common.response.model.IdMappingStringPair;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.uniprotkb.impl.UniProtKBEntryBuilder;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.indexer.uniprot.mockers.UniProtEntryMocker;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.uniprot.UniProtDocument;
import org.uniprot.store.spark.indexer.uniprot.converter.UniProtEntryConverter;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IdMappingRepositoryTest {
    @RegisterExtension static final DataStoreManager storeManager = new DataStoreManager();
    private IdMappingRepository idMappingRepository;

    private static final UniProtKBEntry TEMPLATE_ENTRY =
            UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);

    private final UniProtEntryConverter documentConverter =
            new UniProtEntryConverter(new HashMap<>(), new HashMap<>());

    @BeforeAll
    void beforeAll() throws Exception {
        storeManager.addSolrClient(DataStoreManager.StoreType.UNIPROT, SolrCollection.uniprot);
        storeManager.addSolrClient(DataStoreManager.StoreType.UNIPARC, SolrCollection.uniparc);

        SolrClient kbClient = storeManager.getSolrClient(DataStoreManager.StoreType.UNIPROT);
        SolrClient nonKbClient = storeManager.getSolrClient(DataStoreManager.StoreType.UNIPARC);
        idMappingRepository = new IdMappingRepository(kbClient, nonKbClient);
        addDataSolr(nonKbClient, SolrCollection.uniparc, "UPI", "upi");
        addDataSolr(nonKbClient, SolrCollection.uniref, "UniRef100_UPI", "id");
        addUniProtKBDataSolr(kbClient);
    }

    private void addDataSolr(
            SolrClient client, SolrCollection collection, String idPrefix, String idField)
            throws SolrServerException, IOException {
        var upirefDocs =
                IntStream.range(1, 101)
                        .mapToObj(i -> idPrefix + String.format("%010d", i))
                        .map(id -> new SolrInputDocument(idField, id))
                        .collect(Collectors.toList());
        client.add(collection.name(), upirefDocs);
        client.commit(collection.name());
    }

    @AfterAll
    void afterAll() {
        storeManager.cleanSolr(DataStoreManager.StoreType.UNIPROT);
        storeManager.cleanSolr(DataStoreManager.StoreType.UNIPARC);
        storeManager.close();
    }

    @ParameterizedTest
    @EnumSource(
            value = SolrCollection.class,
            names = {"uniprot", "uniparc", "uniref"})
    void canSendQueryForInterestedCollections(SolrCollection collection)
            throws IOException, SolrServerException {
        IdMappingRepository idMappingRepository =
                new IdMappingRepository(
                        new MockSearchableSolrClient(), new MockSearchableSolrClient());
        assertTrue(idMappingRepository.getAllMappingIds(collection, new ArrayList<>()).isEmpty());
    }

    @Test
    void canGetUniParcMapping() throws SolrServerException, IOException {
        var id1 = "UPI0000000001";
        var id50 = "UPI0000000050";
        var id91 = "UPI0000000091";

        var mappedIdsPairs =
                idMappingRepository.getAllMappingIds(
                        SolrCollection.uniparc, List.of(id1, id50, id91));

        assertAll(
                () -> assertEquals(3, mappedIdsPairs.size()),
                () -> assertEquals(id1, mappedIdsPairs.get(0).getFrom()),
                () -> assertEquals(id1, mappedIdsPairs.get(0).getTo()),
                () -> assertEquals(id50, mappedIdsPairs.get(1).getFrom()),
                () -> assertEquals(id50, mappedIdsPairs.get(1).getTo()),
                () -> assertEquals(id91, mappedIdsPairs.get(2).getFrom()),
                () -> assertEquals(id91, mappedIdsPairs.get(2).getTo()));
    }

    @Test
    void canGetUniRefMapping() throws SolrServerException, IOException {
        var id100 = "UniRef100_UPI0000000100";
        var id99 = "UniRef100_UPI0000000099";
        var id98 = "UniRef100_UPI0000000098";

        var mappedIdsPairs =
                idMappingRepository.getAllMappingIds(
                        SolrCollection.uniref, List.of(id100, id99, id98));

        assertAll(
                () -> assertEquals(3, mappedIdsPairs.size()),
                () -> assertEquals(id100, mappedIdsPairs.get(2).getFrom()),
                () -> assertEquals(id100, mappedIdsPairs.get(2).getTo()),
                () -> assertEquals(id99, mappedIdsPairs.get(1).getFrom()),
                () -> assertEquals(id99, mappedIdsPairs.get(1).getTo()),
                () -> assertEquals(id98, mappedIdsPairs.get(0).getFrom()),
                () -> assertEquals(id98, mappedIdsPairs.get(0).getTo()));
    }

    @Test
    void queryingSameIdMultipleWillReturn1Result() throws SolrServerException, IOException {
        var id50 = "UPI0000000050";

        var mappedIdsPairs =
                idMappingRepository.getAllMappingIds(
                        SolrCollection.uniparc, List.of(id50, id50, id50));

        assertAll(
                () -> assertEquals(1, mappedIdsPairs.size()),
                () -> assertEquals(id50, mappedIdsPairs.get(0).getFrom()),
                () -> assertEquals(id50, mappedIdsPairs.get(0).getTo()));
    }

    @Test
    void queryingSomeIdsNotExistInSolr() throws SolrServerException, IOException {
        var existId = "UPI0000000050";
        var nonExist = "UPI0110000050";

        var mappedIdsPairs =
                idMappingRepository.getAllMappingIds(
                        SolrCollection.uniparc, List.of(nonExist, existId));

        assertAll(
                () -> assertEquals(1, mappedIdsPairs.size()),
                () -> assertEquals(existId, mappedIdsPairs.get(0).getFrom()),
                () -> assertEquals(existId, mappedIdsPairs.get(0).getTo()));
    }

    @Test
    void testGetInactiveUniProtKBEntries() throws SolrServerException, IOException {
        List<String> searchAccessions =
                IntStream.range(1, 16)
                        .mapToObj(i -> String.format("P%05d", i))
                        .collect(Collectors.toList());
        String query = "active:false";
        List<IdMappingStringPair> inactiveEntries =
                idMappingRepository.getAllMappingIds(
                        SolrCollection.uniprot, searchAccessions, query);
        assertEquals(5, inactiveEntries.size());
        List<String> returnedInactiveAccessions =
                inactiveEntries.stream()
                        .map(IdMappingStringPair::getTo)
                        .collect(Collectors.toList());
        assertEquals(
                List.of("P00011", "P00012", "P00013", "P00014", "P00015"),
                returnedInactiveAccessions);
    }

    private void addUniProtKBDataSolr(SolrClient kbClient) throws Exception {
        // create 10 active and 5 inactive kb entries in solr
        saveEntries(kbClient, 1, 10, false);
        saveEntries(kbClient, 11, 15, true);
    }

    private void saveEntries(SolrClient kbClient, int start, int end, boolean inactive)
            throws Exception {
        for (int i = start; i <= end; i++) {
            saveEntry(kbClient, i, inactive);
        }
        kbClient.commit(SolrCollection.uniprot.name());
    }

    private void saveEntry(SolrClient kbClient, int i, boolean inactive)
            throws SolrServerException, IOException {
        UniProtKBEntryBuilder entryBuilder = UniProtKBEntryBuilder.from(TEMPLATE_ENTRY);
        String acc = String.format("P%05d", i);
        entryBuilder.primaryAccession(acc);
        UniProtKBEntry uniProtKBEntry = entryBuilder.build();
        UniProtDocument doc = documentConverter.convert(uniProtKBEntry);
        kbClient.addBean(SolrCollection.uniprot.name(), doc);
        // update the inactive flag and update solr doc
        if (inactive) {
            doc.active = false;
            kbClient.addBean(SolrCollection.uniprot.name(), doc);
        }
    }
}
