package org.uniprot.api.uniparc.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.rest.controller.AbstractGetByIdsPostControllerIT;
import org.uniprot.api.rest.request.IdsSearchRequest;
import org.uniprot.api.rest.respository.facet.impl.UniParcFacetConfig;
import org.uniprot.api.uniparc.common.repository.store.crossref.UniParcCrossReferenceStoreClient;
import org.uniprot.api.uniparc.request.UniParcIdsPostRequest;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.uniparc.UniParcEntryLight;
import org.uniprot.core.uniparc.impl.UniParcCrossReferencePair;
import org.uniprot.core.xml.jaxb.uniparc.Entry;
import org.uniprot.core.xml.uniparc.UniParcEntryConverter;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.indexer.converters.UniParcDocumentConverter;
import org.uniprot.store.indexer.uniparc.mockers.UniParcCrossReferenceMocker;
import org.uniprot.store.indexer.uniparc.mockers.UniParcEntryMocker;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.uniparc.UniParcDocument;

/**
 * @author sahmad
 * @created 28/07/2021
 */
@ActiveProfiles(profiles = "offline")
@WebMvcTest(UniParcController.class)
@ExtendWith(value = SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UniParcGetByUpisPostIT extends AbstractGetByIdsPostControllerIT {
    private static final String UPI_PREF = "UPI00000000";
    private static final String GET_BY_UPIS_PATH = "/uniparc/upis";
    private final UniParcDocumentConverter documentConverter =
            new UniParcDocumentConverter(TaxonomyRepoMocker.getTaxonomyRepo(), new HashMap<>());
    private static final String TEST_IDS =
            "UPI0000000004,UPI0000000003,UPI0000000002,UPI0000000001,UPI0000000005,"
                    + "UPI0000000006,UPI0000000007,UPI0000000010,UPI0000000009,UPI0000000008";
    private static final String[] TEST_IDS_ARRAY = {
        "UPI0000000004",
        "UPI0000000003",
        "UPI0000000002",
        "UPI0000000001",
        "UPI0000000005",
        "UPI0000000006",
        "UPI0000000007",
        "UPI0000000010",
        "UPI0000000009",
        "UPI0000000008"
    };

    @Autowired UniProtStoreClient<UniParcEntryLight> storeClient;
    @Autowired private UniParcCrossReferenceStoreClient crossRefStoreClient;
    @Autowired private MockMvc mockMvc;
    @Autowired private FacetTupleStreamTemplate facetTupleStreamTemplate;
    @Autowired private TupleStreamTemplate tupleStreamTemplate;
    @Autowired private UniParcFacetConfig facetConfig;

    @Value("${voldemort.uniparc.cross.reference.groupSize:#{null}}")
    private Integer xrefGroupSize;

    @BeforeAll
    void saveEntriesInSolrAndStore() throws Exception {
        saveEntries();
    }

    @Override
    protected List<SolrCollection> getSolrCollections() {
        return List.of(SolrCollection.uniparc);
    }

    @Override
    protected TupleStreamTemplate getTupleStreamTemplate() {
        return this.tupleStreamTemplate;
    }

    @Override
    protected FacetTupleStreamTemplate getFacetTupleStreamTemplate() {
        return this.facetTupleStreamTemplate;
    }

    @Override
    protected IdsSearchRequest getInvalidDownloadRequest() {
        UniParcIdsPostRequest idsSearchRequest = new UniParcIdsPostRequest();
        idsSearchRequest.setUpis(getCommaSeparatedIds() + ",INVALID , INVALID2");
        idsSearchRequest.setFields("invalid, invalid1");
        return idsSearchRequest;
    }

    @Override
    protected String[] getErrorMessage() {
        return new String[] {
            "'upis' is a required parameter",
        };
    }

    @Override
    protected IdsSearchRequest getIdsDownloadRequest() {
        UniParcIdsPostRequest idsSearchRequest = new UniParcIdsPostRequest();
        idsSearchRequest.setUpis(getCommaSeparatedIds());
        return idsSearchRequest;
    }

    @Override
    protected IdsSearchRequest getIdsDownloadWithFieldsRequest() {
        UniParcIdsPostRequest idsSearchRequest = new UniParcIdsPostRequest();
        idsSearchRequest.setUpis(getCommaSeparatedIds());
        idsSearchRequest.setFields(getCommaSeparatedReturnFields());
        return idsSearchRequest;
    }

    @Override
    protected String getCommaSeparatedIds() {
        return TEST_IDS;
    }

    @Override
    protected MockMvc getMockMvc() {
        return this.mockMvc;
    }

    @Override
    protected String getGetByIdsPath() {
        return GET_BY_UPIS_PATH;
    }

    @Override
    protected String getCommaSeparatedReturnFields() {
        return "gene,organism_id,CDD";
    }

    @Override
    protected List<ResultMatcher> getResultsResultMatchers() {
        ResultMatcher rm1 = jsonPath("$.results.*.uniParcId", contains(TEST_IDS_ARRAY));
        ResultMatcher rm2 = jsonPath("$.results[0].crossReferenceCount", is(25));
        ResultMatcher rm3 = jsonPath("$.results.*.sequence").exists();
        ResultMatcher rm4 = jsonPath("$.results.*.sequence", notNullValue());
        ResultMatcher rm5 = jsonPath("$.results[0].sequenceFeatures", iterableWithSize(12));
        return List.of(rm1, rm2, rm3, rm4, rm5);
    }

    @Override
    protected List<ResultMatcher> getIdsAsResultMatchers() {
        return Arrays.stream(TEST_IDS_ARRAY)
                .map(id -> content().string(containsString(id)))
                .collect(Collectors.toList());
    }

    @Override
    protected List<ResultMatcher> getFieldsResultMatchers() {
        ResultMatcher rm1 = jsonPath("$.results.*.uniParcId", contains(TEST_IDS_ARRAY));
        ResultMatcher rm2 = jsonPath("$.results.*.sequence").doesNotExist();
        ResultMatcher rm3 = jsonPath("$.results.*.geneNames").exists();
        ResultMatcher rm4 = jsonPath("$.results.*.organisms").exists();
        ResultMatcher rm5 = jsonPath("$.results.*.sequenceFeatures.*.database").exists();
        ResultMatcher rm6 = jsonPath("$.results[0].sequenceFeatures[0].database", is("CDD"));
        return List.of(rm1, rm2, rm3, rm4, rm5, rm6);
    }

    @Override
    protected String[] getErrorMessages() {
        return new String[] {
            "UPI 'INVALID2' has invalid format. It should be a valid UniParc id.",
            "Only '10' upis are allowed in each request.",
            "Invalid fields parameter value 'invalid'",
            "UPI 'INVALID' has invalid format. It should be a valid UniParc id.",
            "Invalid fields parameter value 'invalid1'"
        };
    }

    @Override
    protected String getJsonRequestBodyWithFacets() {
        String facets = String.join(",", facetConfig.getFacetNames());
        StringBuilder builder = new StringBuilder("{");
        builder.append("\"upis\":\"" + getCommaSeparatedIds() + "\",");
        builder.append("\"facets\":\"" + facets + "\"");
        builder.append("}");
        return builder.toString();
    }

    @Override
    protected String getJsonRequestBodyWithFacetFilter() {
        String facetFilter = "database_facet:1";
        StringBuilder builder = new StringBuilder("{");
        builder.append("\"upis\":\"" + getCommaSeparatedIds() + "\",");
        builder.append("\"facetFilter\":\"" + facetFilter + "\"");
        builder.append("}");
        return builder.toString();
    }

    @Override
    protected String getJsonRequestBodyWithDownloadParam() {
        String download = "false";
        StringBuilder builder = new StringBuilder("{");
        builder.append("\"upis\":\"" + getCommaSeparatedIds() + "\",");
        builder.append("\"download\":\"" + download + "\"");
        builder.append("}");
        return builder.toString();
    }

    private void saveEntries() throws Exception {
        for (int i = 1; i <= 10; i++) {
            saveEntry(i);
        }
        cloudSolrClient.commit(SolrCollection.uniparc.name());
    }

    private void saveEntry(int qualifier) throws Exception {
        int xrefCount = 25;
        UniParcEntry entry = UniParcEntryMocker.createUniParcEntry(qualifier, UPI_PREF, xrefCount);
        UniParcEntryConverter converter = new UniParcEntryConverter();
        Entry xmlEntry = converter.toXml(entry);
        UniParcDocument doc = documentConverter.convert(xmlEntry);
        cloudSolrClient.addBean(SolrCollection.uniparc.name(), doc);
        UniParcEntryLight uniParcEntryLight =
                UniParcEntryMocker.createUniParcEntryLight(qualifier, UPI_PREF, xrefCount);
        storeClient.saveEntry(uniParcEntryLight);
        List<UniParcCrossReferencePair> crossReferences =
                UniParcCrossReferenceMocker.createUniParcCrossReferencePairs(
                        uniParcEntryLight.getUniParcId(), qualifier, xrefCount, xrefGroupSize);
        crossReferences.forEach(pair -> crossRefStoreClient.saveEntry(pair.getKey(), pair));
    }
}
