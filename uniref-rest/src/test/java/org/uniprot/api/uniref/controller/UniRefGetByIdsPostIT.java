package org.uniprot.api.uniref.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.rest.controller.AbstractGetByIdsPostControllerIT;
import org.uniprot.api.rest.request.IdsSearchRequest;
import org.uniprot.api.rest.respository.facet.impl.UniRefFacetConfig;
import org.uniprot.api.uniref.request.UniRefIdsDownloadRequest;
import org.uniprot.core.uniref.UniRefEntry;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.core.uniref.UniRefType;
import org.uniprot.core.xml.jaxb.uniref.Entry;
import org.uniprot.core.xml.uniref.UniRefEntryConverter;
import org.uniprot.core.xml.uniref.UniRefEntryLightConverter;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.indexer.uniref.UniRefDocumentConverter;
import org.uniprot.store.indexer.uniref.mockers.UniRefEntryMocker;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.uniref.UniRefDocument;

/**
 * @author sahmad
 * @created 28/07/2021
 */
@ActiveProfiles(profiles = "offline")
@WebMvcTest(UniRefEntryLightController.class)
@ExtendWith(value = SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UniRefGetByIdsPostIT extends AbstractGetByIdsPostControllerIT {
    private static final String GET_BY_IDS_PATH = "/uniref/ids";
    private final UniRefDocumentConverter documentConverter =
            new UniRefDocumentConverter(TaxonomyRepoMocker.getTaxonomyRepo());
    private static final String TEST_IDS =
            "UniRef50_P03901,UniRef90_P03901,UniRef100_P03901,UniRef50_P03902,"
                    + "UniRef90_P03902,UniRef100_P03902,UniRef50_P03903,UniRef90_P03903,UniRef100_P03903,UniRef50_P03904";
    private static final String[] TEST_IDS_ARRAY = {
        "UniRef50_P03901",
        "UniRef90_P03901",
        "UniRef100_P03901",
        "UniRef50_P03902",
        "UniRef90_P03902",
        "UniRef100_P03902",
        "UniRef50_P03903",
        "UniRef90_P03903",
        "UniRef100_P03903",
        "UniRef50_P03904"
    };
    @Autowired UniProtStoreClient<UniRefEntryLight> storeClient;
    @Autowired private MockMvc mockMvc;
    @Autowired private FacetTupleStreamTemplate facetTupleStreamTemplate;
    @Autowired private TupleStreamTemplate tupleStreamTemplate;
    @Autowired private UniRefFacetConfig facetConfig;

    @BeforeAll
    void saveEntriesInSolrAndStore() throws Exception {
        saveEntries();
    }

    private void saveEntries() throws Exception {
        for (int i = 1; i <= 4; i++) {
            saveEntry(i, UniRefType.UniRef50);
            saveEntry(i, UniRefType.UniRef90);
            saveEntry(i, UniRefType.UniRef100);
        }
        cloudSolrClient.commit(SolrCollection.uniref.name());
    }

    private void saveEntry(int i, UniRefType type) throws Exception {
        UniRefEntry entry = UniRefEntryMocker.createEntry(i, type);
        UniRefEntryConverter converter = new UniRefEntryConverter();
        Entry xmlEntry = converter.toXml(entry);
        UniRefEntryLightConverter unirefLightConverter = new UniRefEntryLightConverter();
        UniRefEntryLight entryLight = unirefLightConverter.fromXml(xmlEntry);
        UniRefDocument doc = documentConverter.convert(xmlEntry);
        cloudSolrClient.addBean(SolrCollection.uniref.name(), doc);
        storeClient.saveEntry(entryLight);
    }

    @Override
    protected List<SolrCollection> getSolrCollections() {
        return List.of(SolrCollection.uniref);
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
        UniRefIdsDownloadRequest idsSearchRequest = new UniRefIdsDownloadRequest();
        idsSearchRequest.setIds(getCommaSeparatedIds() + ",INVALID , INVALID2");
        idsSearchRequest.setDownload("INVALID");
        idsSearchRequest.setFields("invalid, invalid1");
        return idsSearchRequest;
    }

    @Override
    protected IdsSearchRequest getIdsDownloadRequest() {
        UniRefIdsDownloadRequest idsSearchRequest = new UniRefIdsDownloadRequest();
        idsSearchRequest.setIds(getCommaSeparatedIds());
        idsSearchRequest.setDownload("true");
        return idsSearchRequest;
    }

    @Override
    protected IdsSearchRequest getIdsDownloadWithFieldsRequest() {
        UniRefIdsDownloadRequest idsSearchRequest = new UniRefIdsDownloadRequest();
        idsSearchRequest.setIds(getCommaSeparatedIds());
        idsSearchRequest.setDownload("true");
        idsSearchRequest.setFields(getCommaSeparatedReturnFields());
        return idsSearchRequest;
    }

    @Override
    protected String getCommaSeparatedIds() {
        return TEST_IDS;
    }

    @Override
    protected IdsSearchRequest getIdsDownloadWithFieldsAndSizeRequest() {
        UniRefIdsDownloadRequest idsSearchRequest = new UniRefIdsDownloadRequest();
        idsSearchRequest.setIds(getCommaSeparatedIds());
        idsSearchRequest.setDownload("true");
        idsSearchRequest.setFields(getCommaSeparatedReturnFields());
        idsSearchRequest.setSize(4);
        return idsSearchRequest;
    }

    @Override
    protected MockMvc getMockMvc() {
        return this.mockMvc;
    }

    @Override
    protected String getGetByIdsPath() {
        return GET_BY_IDS_PATH;
    }

    @Override
    protected String getCommaSeparatedReturnFields() {
        return "id,name,length,organism_id";
    }

    @Override
    protected List<ResultMatcher> getResultsResultMatchers() {
        ResultMatcher rm1 = jsonPath("$.results.*.id", contains(TEST_IDS_ARRAY));
        ResultMatcher rm2 = jsonPath("$.results.*.name").exists();
        ResultMatcher rm3 = jsonPath("$.results.*.updated").exists();
        ResultMatcher rm4 = jsonPath("$.results.*.entryType").exists();
        ResultMatcher rm5 = jsonPath("$.results.*.memberCount").exists();
        ResultMatcher rm6 = jsonPath("$.results.*.organismCount").exists();
        ResultMatcher rm7 = jsonPath("$.results.*.seedId").exists();
        ResultMatcher rm8 = jsonPath("$.results.*.commonTaxon").exists();
        ResultMatcher rm9 = jsonPath("$.results.*.representativeMember").exists();
        ResultMatcher rm10 = jsonPath("$.results[0].memberIdTypes").isArray();
        ResultMatcher rm11 = jsonPath("$.results[0].members").isArray();
        ResultMatcher rm12 = jsonPath("$.results[0].organisms").isArray();
        ResultMatcher rm13 = jsonPath("$.results[0].goTerms").isArray();
        return List.of(rm1, rm2, rm3, rm4, rm5, rm6, rm7, rm8, rm9, rm10, rm11, rm12, rm13);
    }

    @Override
    protected List<ResultMatcher> getIdsAsResultMatchers() {
        return Arrays.stream(TEST_IDS_ARRAY)
                .map(id -> content().string(containsString(id)))
                .collect(Collectors.toList());
    }

    @Override
    protected List<ResultMatcher> getFieldsResultMatchers() {
        ResultMatcher rm1 = jsonPath("$.results.*.id", contains(TEST_IDS_ARRAY));
        ResultMatcher rm2 = jsonPath("$.results.*.name").exists();
        ResultMatcher rm3 = jsonPath("$.results.*.updated").doesNotExist();
        ResultMatcher rm4 = jsonPath("$.results.*.entryType").doesNotExist();
        ResultMatcher rm5 = jsonPath("$.results.*.memberCount").doesNotExist();
        ResultMatcher rm6 = jsonPath("$.results.*.organismCount").doesNotExist();
        ResultMatcher rm7 = jsonPath("$.results.*.seedId").doesNotExist();
        ResultMatcher rm8 = jsonPath("$.results.*.commonTaxon").doesNotExist();
        ResultMatcher rm9 = jsonPath("$.results.*.representativeMember").exists();
        ResultMatcher rm10 = jsonPath("$.results[0].memberIdTypes").doesNotExist();
        ResultMatcher rm11 = jsonPath("$.results[0].members").isArray();
        ResultMatcher rm12 = jsonPath("$.results[0].organisms").isArray();
        ResultMatcher rm13 = jsonPath("$.results[0].goTerms").doesNotExist();
        return List.of(rm1, rm2, rm3, rm4, rm5, rm6, rm7, rm8, rm9, rm10, rm11, rm12, rm13);
    }

    @Override
    protected List<ResultMatcher> getFirstPageResultMatchers() {
        ResultMatcher rm1 =
                jsonPath(
                        "$.results.*.id",
                        contains(List.of(TEST_IDS_ARRAY).subList(0, 4).toArray()));
        return List.of(rm1);
    }

    @Override
    protected List<ResultMatcher> getSecondPageResultMatchers() {
        ResultMatcher rm1 =
                jsonPath(
                        "$.results.*.id",
                        contains(List.of(TEST_IDS_ARRAY).subList(4, 8).toArray()));
        return List.of(rm1);
    }

    @Override
    protected List<ResultMatcher> getThirdPageResultMatchers() {
        ResultMatcher rm1 =
                jsonPath(
                        "$.results.*.id",
                        contains(List.of(TEST_IDS_ARRAY).subList(8, 10).toArray()));
        return List.of(rm1);
    }

    @Override
    protected String[] getErrorMessages() {
        return new String[] {
            "Invalid fields parameter value 'invalid1'",
            "UniRef id 'INVALID2' has invalid format. It should be a valid UniRef id.",
            "Invalid fields parameter value 'invalid'",
            "UniRef id 'INVALID' has invalid format. It should be a valid UniRef id.",
            "Only '10' UniRef ids are allowed in each request.",
            "The 'download' parameter has invalid format. It should set to true."
        };
    }

    @Override
    protected String[] getErrorMessagesForEmptyJson() {
        return new String[] {"'download' is a required parameter", "'ids' is a required parameter"};
    }

    @Override
    protected String getJsonRequestBodyWithFacets() {
        String facets = String.join(",", facetConfig.getFacetNames());
        StringBuilder builder = new StringBuilder("{");
        builder.append("\"ids\":\"" + getCommaSeparatedIds() + "\",");
        builder.append("\"download\":\"true\",");
        builder.append("\"facets\":\"" + facets + "\"");
        builder.append("}");
        return builder.toString();
    }

    @Override
    protected String getJsonRequestBodyWithFacetFilter() {
        String facetFilter = "identity:0.5 OR identity:0.9 OR identity:1.0";
        StringBuilder builder = new StringBuilder("{");
        builder.append("\"ids\":\"" + getCommaSeparatedIds() + "\",");
        builder.append("\"download\":\"true\",");
        builder.append("\"facetFilter\":\"" + facetFilter + "\"");
        builder.append("}");
        return builder.toString();
    }
}
