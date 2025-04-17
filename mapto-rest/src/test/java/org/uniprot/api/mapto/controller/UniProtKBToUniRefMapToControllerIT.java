package org.uniprot.api.mapto.controller;

import com.jayway.jsonpath.JsonPath;
import org.apache.solr.client.solrj.SolrClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.common.repository.stream.store.uniprotkb.TaxonomyLineageRepository;
import org.uniprot.api.idmapping.common.service.TestConfig;
import org.uniprot.api.mapto.MapToREST;
import org.uniprot.api.mapto.common.RedisConfiguration;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.api.uniprotkb.common.repository.UniProtKBDataStoreTestConfig;
import org.uniprot.api.uniprotkb.common.repository.search.UniprotQueryRepository;
import org.uniprot.api.uniprotkb.common.utils.UniProtKBAsyncDownloadUtils;
import org.uniprot.api.uniref.common.repository.UniRefDataStoreTestConfig;
import org.uniprot.api.uniref.common.repository.search.UniRefQueryRepository;
import org.uniprot.api.uniref.common.util.UniRefAsyncDownloadUtils;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.search.SolrCollection;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.uniprot.api.mapto.controller.UniProtKBUniRefMapToController.UNIPROTKB_UNIREF;
import static org.uniprot.store.search.SolrCollection.*;

@ActiveProfiles(profiles = {"offline", "idmapping"})
@WebMvcTest({UniProtKBUniRefMapToController.class})
@ContextConfiguration(
        classes = {
                TestConfig.class,
                UniRefDataStoreTestConfig.class,
                UniProtKBDataStoreTestConfig.class,
                MapToREST.class,
                ErrorHandlerConfig.class,
                RedisConfiguration.class
        })
@ExtendWith(SpringExtension.class)
@AutoConfigureWebClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UniProtKBToUniRefMapToControllerIT extends MapToControllerIT {

    @Autowired
    private UniprotQueryRepository uniprotQueryRepository;
    @Autowired
    private UniRefQueryRepository uniRefQueryRepository;
    @Autowired
    private TaxonomyLineageRepository taxRepository;

    @Autowired
    @Qualifier("uniProtKBTupleStream")
    private TupleStreamTemplate uniProtKBTupleStreamTemplate;

    @Qualifier("uniProtKBFacetTupleStreamTemplate")
    @Autowired
    private FacetTupleStreamTemplate uniProtKBFacetTupleStreamTemplate;

    @Qualifier("uniRefFacetTupleStreamTemplate")
    @Autowired
    private FacetTupleStreamTemplate unirefFacetTupleStreamTemplate;

    @Autowired
    @Qualifier("uniRefTupleStreamTemplate")
    private TupleStreamTemplate unirefTupleStreamTemplate;

    @Autowired
    @Qualifier("uniProtKBSolrClient")
    private SolrClient uniProtKBSolrClient;

    @Qualifier("uniProtStoreClient")
    @Autowired
    private UniProtStoreClient<UniProtKBEntry> uniProtKBStoreClient;

    @Qualifier("uniRefLightStoreClient")
    @Autowired
    private UniProtStoreClient<UniRefEntryLight> uniRefStoreClient;

    @BeforeAll
    public void runSaveEntriesInSolrAndStore() throws Exception {
        UniProtKBAsyncDownloadUtils.saveEntriesInSolrAndStore(
                uniprotQueryRepository,
                cloudSolrClient,
                uniProtKBSolrClient,
                uniProtKBStoreClient,
                taxRepository);
        UniRefAsyncDownloadUtils.saveEntriesInSolrAndStore(
                uniRefQueryRepository, cloudSolrClient, solrClient, uniRefStoreClient);
    }

    @Override
    protected Map<String, String> getSortQuery() {
        return Map.of("query", "*:*",
                "sort", "id desc");
    }

    @Override
    protected Map<String, String> getFilterQuery() {
        return Map.of("query", "id:UniRef90_P03903");
    }

    @Override
    protected void verifyResultsWithLimit(String resultsJson) {
        List<String> organisms = JsonPath.read(resultsJson, "$.results.*.id");
        assertTrue(organisms.containsAll(List.of(
                "UniRef100_P03901", "UniRef100_P03902", "UniRef100_P03903", "UniRef100_P03904", "UniRef50_P03901"
        )));
        assertEquals(5, organisms.size());
        String accession = JsonPath.read(resultsJson, "$.results[4].representativeMember.accessions[0]");
        assertEquals("P12301", accession);
        String memberId = JsonPath.read(resultsJson, "$.results[4].representativeMember.memberId");
        assertEquals("P12301_HUMAN", memberId);

    }

    @Override
    protected String getPath() {
        return UNIPROTKB_UNIREF;
    }

    @Override
    protected void verifyResultsWithSort(String resultsJson) {
        List<String> organisms = JsonPath.read(resultsJson, "$.results.*.id");
        List<String> expected = List.of(
                "UniRef90_P03904", "UniRef90_P03903", "UniRef90_P03902", "UniRef90_P03901",
                "UniRef50_P03904", "UniRef50_P03903", "UniRef50_P03902", "UniRef50_P03901",
                "UniRef100_P03904", "UniRef100_P03903", "UniRef100_P03902", "UniRef100_P03901"
        );
        assertEquals(new LinkedList<>(organisms), new LinkedList<>(expected));
        assertEquals(12, organisms.size());
        String accession = JsonPath.read(resultsJson, "$.results[11].representativeMember.accessions[0]");
        assertEquals("P12301", accession);
        String memberId = JsonPath.read(resultsJson, "$.results[11].representativeMember.memberId");
        assertEquals("P12301_HUMAN", memberId);
    }

    @Override
    protected void verifyResultsWithFilter(String resultsJson) {
        List<String> organisms = JsonPath.read(resultsJson, "$.results.*.id");
        List<String> expected = List.of("UniRef90_P03903");
        assertEquals(new LinkedList<>(organisms), new LinkedList<>(expected));
        assertEquals(1, organisms.size());
        String accession = JsonPath.read(resultsJson, "$.results[0].representativeMember.accessions[0]");
        assertEquals("P12303", accession);
        String memberId = JsonPath.read(resultsJson, "$.results[0].representativeMember.memberId");
        assertEquals("P12303_HUMAN", memberId);
    }

    @Override
    protected void verifyResults(String resultsJson) {
        List<String> organisms = JsonPath.read(resultsJson, "$.results.*.id");
        assertTrue(organisms.containsAll(List.of(
                "UniRef100_P03901", "UniRef100_P03902", "UniRef100_P03903", "UniRef100_P03904",
                "UniRef90_P03901", "UniRef90_P03902", "UniRef90_P03903", "UniRef90_P03904",
                "UniRef50_P03901", "UniRef50_P03902", "UniRef50_P03903", "UniRef50_P03904"
        )));
        assertEquals(12, organisms.size());
        String accession = JsonPath.read(resultsJson, "$.results[0].representativeMember.accessions[0]");
        assertEquals("P12301", accession);
        String memberId = JsonPath.read(resultsJson, "$.results[0].representativeMember.memberId");
        assertEquals("P12301_HUMAN", memberId);
    }

    @Override
    protected String getDownloadAPIsBasePath() {
        return UniProtKBUniRefMapToController.RESOURCE_PATH;
    }

    @Override
    protected List<SolrCollection> getSolrCollections() {
        return List.of(uniprot, uniref, taxonomy);
    }

    @Override
    protected Collection<TupleStreamTemplate> getTupleStreamTemplates() {
        return List.of(uniProtKBTupleStreamTemplate, unirefTupleStreamTemplate);
    }

    @Override
    protected Collection<FacetTupleStreamTemplate> getFacetTupleStreamTemplates() {
        return List.of(uniProtKBFacetTupleStreamTemplate, unirefFacetTupleStreamTemplate);
    }

    @Override
    protected int getTotalEntries() {
        return 12;
    }
}
