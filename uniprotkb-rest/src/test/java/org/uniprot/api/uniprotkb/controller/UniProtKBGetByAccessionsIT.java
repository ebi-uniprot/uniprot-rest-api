package org.uniprot.api.uniprotkb.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.rest.controller.AbstractGetByIdsControllerIT;
import org.uniprot.api.rest.respository.facet.impl.UniProtKBFacetConfig;
import org.uniprot.api.uniprotkb.UniProtKBREST;
import org.uniprot.api.uniprotkb.repository.DataStoreTestConfig;
import org.uniprot.core.gene.Gene;
import org.uniprot.core.gene.GeneName;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.uniprotkb.UniProtKBEntryType;
import org.uniprot.core.uniprotkb.impl.GeneBuilder;
import org.uniprot.core.uniprotkb.impl.GeneNameBuilder;
import org.uniprot.core.uniprotkb.impl.UniProtKBEntryBuilder;
import org.uniprot.cv.chebi.ChebiRepo;
import org.uniprot.cv.ec.ECRepo;
import org.uniprot.cv.go.GORepo;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.indexer.uniprot.mockers.PathwayRepoMocker;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.indexer.uniprot.mockers.UniProtEntryMocker;
import org.uniprot.store.indexer.uniprotkb.converter.UniProtEntryConverter;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.uniprot.UniProtDocument;

/**
 * @author lgonzales
 * @since 2019-07-10
 */
@ContextConfiguration(classes = {DataStoreTestConfig.class, UniProtKBREST.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(UniProtKBPublicationController.class)
@AutoConfigureWebClient
@ExtendWith(value = {SpringExtension.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UniProtKBGetByAccessionsIT extends AbstractGetByIdsControllerIT {

    private static final String GET_BY_ACCESSIONS_PATH = "/uniprotkb/accessions";

    private static final UniProtKBEntry TEMPLATE_ENTRY =
            UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);

    private final UniProtEntryConverter documentConverter =
            new UniProtEntryConverter(
                    TaxonomyRepoMocker.getTaxonomyRepo(),
                    Mockito.mock(GORepo.class),
                    PathwayRepoMocker.getPathwayRepo(),
                    mock(ChebiRepo.class),
                    mock(ECRepo.class),
                    new HashMap<>());

    private static final String TEST_IDS =
            "p00003,P00002,P00001,P00007,P00006,P00005,P00004,P00008,P00010,P00009";
    public static final String[] TEST_IDS_ARRAY = {
        "P00003", "P00002", "P00001", "P00007", "P00006", "P00005", "P00004", "P00008", "P00010",
        "P00009"
    };
    private static final String[] TEST_IDS_ARRAY_SORTED = {
        "P00001", "P00002", "P00003", "P00004", "P00005", "P00006", "P00007", "P00008", "P00009",
        "P00010"
    };
    private static final String MISSING_ID1 = "Q00001";
    private static final String MISSING_ID2 = "Q00002";

    @Autowired private UniProtStoreClient<UniProtKBEntry> storeClient;

    @Autowired private FacetTupleStreamTemplate facetTupleStreamTemplate;
    @Autowired private TupleStreamTemplate tupleStreamTemplate;

    @Autowired private MockMvc mockMvc;

    @Autowired private UniProtKBFacetConfig facetConfig;

    @BeforeAll
    void saveEntriesInSolrAndStore() throws Exception {
        char prefix = 'z';
        for (int i = 1; i <= 10; i++) {
            UniProtKBEntryBuilder entryBuilder = UniProtKBEntryBuilder.from(TEMPLATE_ENTRY);
            String acc = String.format("P%05d", i);
            entryBuilder.primaryAccession(acc);
            if (i % 2 == 0) {
                entryBuilder.entryType(UniProtKBEntryType.SWISSPROT);
            } else {
                entryBuilder.entryType(UniProtKBEntryType.TREMBL);
            }

            // update gene name
            List<Gene> genes = new ArrayList<>(TEMPLATE_ENTRY.getGenes());
            Gene gene = genes.get(0);
            GeneBuilder geneBuilder = GeneBuilder.from(gene);
            GeneNameBuilder gnBuilder = GeneNameBuilder.from(gene.getGeneName());
            GeneName gn = gnBuilder.value(prefix + gene.getGeneName().getValue()).build();
            geneBuilder.geneName(gn);
            genes.remove(0);
            genes.add(0, geneBuilder.build());
            entryBuilder.genesSet(genes);

            UniProtKBEntry uniProtKBEntry = entryBuilder.build();

            UniProtDocument convert = documentConverter.convert(uniProtKBEntry);

            cloudSolrClient.addBean(SolrCollection.uniprot.name(), convert);
            storeClient.saveEntry(uniProtKBEntry);
            prefix--;
        }
        cloudSolrClient.commit(SolrCollection.uniprot.name());
    }

    @Test
    void getByIdsSortByProteinNameWithCorrectValuesSuccess() throws Exception {
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getGetByIdsPath())
                                        .header(
                                                org.apache.http.HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON)
                                        .param(getRequestParamName(), getCommaSeparatedIds())
                                        .param("sort", "gene asc")
                                        .param("size", "10"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(10)))
                .andExpect(getReverseSortedIdResultMatcher())
                .andExpect(jsonPath("$.facets").doesNotExist());
    }

    @Override
    protected String getIdSortField() {
        return "accession";
    }

    @Override
    protected String getCommaSeparatedIds() {
        return TEST_IDS;
    }

    @Override
    protected String getCommaSeparatedNIds(int n) {
        return String.join(",", Arrays.asList(TEST_IDS_ARRAY).subList(0, n));
    }

    @Override
    protected String getCommaSeparatedMixedIds() {
        // 6 present and 2 missing ids
        String csvs = String.join(",", Arrays.asList(TEST_IDS_ARRAY).subList(0, 6));
        return MISSING_ID1 + "," + csvs + "," + MISSING_ID2;
    }

    @Override
    protected String getCommaSeparatedMissingIds() {
        return MISSING_ID1 + "," + MISSING_ID2;
    }

    @Override
    protected String getCommaSeparatedFacets() {
        return String.join(",", facetConfig.getFacetNames());
    }

    @Override
    protected List<ResultMatcher> getResultsResultMatchers() {
        ResultMatcher rm1 = jsonPath("$.results.*.primaryAccession", contains(TEST_IDS_ARRAY));
        ResultMatcher rm2 =
                jsonPath("$.results[0].entryType", equalTo("UniProtKB unreviewed (TrEMBL)"));
        ResultMatcher rm3 = jsonPath("$.results[0].uniProtkbId", equalTo("FGFR2_HUMAN"));
        return List.of(rm1, rm2, rm3);
    }

    @Override
    protected List<ResultMatcher> getFacetsResultMatchers() {
        ResultMatcher rm1 = jsonPath("$.facets.size()", is(9));
        ResultMatcher rm2 =
                jsonPath(
                        "$.facets.*.label",
                        containsInAnyOrder(
                                "3D Structure",
                                "Proteins with",
                                "Fragment",
                                "Protein existence",
                                "Sequence length",
                                "Status",
                                "Annotation score",
                                "Popular organisms",
                                "Proteomes"));
        return List.of(rm1, rm2);
    }

    @Override
    protected List<ResultMatcher> getIdsAsResultMatchers() {
        return Arrays.stream(TEST_IDS_ARRAY)
                .map(id -> content().string(containsString(id)))
                .collect(Collectors.toList());
    }

    @Override
    protected MockMvc getMockMvc() {
        return this.mockMvc;
    }

    @Override
    protected String getGetByIdsPath() {
        return GET_BY_ACCESSIONS_PATH;
    }

    @Override
    protected String getRequestParamName() {
        return "accessions";
    }

    @Override
    protected String getCommaSeparatedReturnFields() {
        return "gene_names,organism_name";
    }

    @Override
    protected List<ResultMatcher> getFieldsResultMatchers() {
        ResultMatcher rm1 = jsonPath("$.results.*.primaryAccession", contains(TEST_IDS_ARRAY));
        ResultMatcher rm2 = jsonPath("$.results.*.organism").exists();
        ResultMatcher rm3 = jsonPath("$.results.*.genes").exists();
        ResultMatcher rm4 = jsonPath("$.results.*.sequence").doesNotExist();
        ResultMatcher rm5 = jsonPath("$.results.*.comments").doesNotExist();
        ResultMatcher rm6 =
                jsonPath("$.results[0].organism.scientificName", equalTo("Homo sapiens"));
        ResultMatcher rm7 = jsonPath("$.results[0].organism.commonName", equalTo("Human"));
        ResultMatcher rm8 = jsonPath("$.results[0].organism.taxonId", equalTo(9606));
        ResultMatcher rm9 =
                jsonPath(
                        "$.results[0].organism.lineage",
                        equalTo(TEMPLATE_ENTRY.getOrganism().getLineages()));
        return List.of(rm1, rm2, rm3, rm4, rm5, rm6, rm7, rm8, rm9);
    }

    @Override
    protected List<ResultMatcher> getFirstPageResultMatchers() {
        ResultMatcher rm1 =
                jsonPath(
                        "$.results.*.primaryAccession",
                        contains(List.of(TEST_IDS_ARRAY).subList(0, 4).toArray()));
        ResultMatcher rm2 = jsonPath("$.facets", iterableWithSize(9));
        ResultMatcher rm3 =
                jsonPath(
                        "$.facets.*.label",
                        containsInAnyOrder(
                                "3D Structure",
                                "Proteins with",
                                "Fragment",
                                "Protein existence",
                                "Sequence length",
                                "Status",
                                "Annotation score",
                                "Popular organisms",
                                "Proteomes"));
        ResultMatcher rm4 =
                jsonPath(
                        "$.facets.*.name",
                        containsInAnyOrder(
                                "structure_3d",
                                "proteins_with",
                                "fragment",
                                "existence",
                                "length",
                                "reviewed",
                                "annotation_score",
                                "model_organism",
                                "proteome"));
        ResultMatcher rm5 = jsonPath("$.facets.*.values").exists();
        ResultMatcher rm6 = jsonPath("$.facets.*.values").isArray();
        return List.of(rm1, rm2, rm3, rm4, rm5, rm6);
    }

    @Override
    protected List<ResultMatcher> getSecondPageResultMatchers() {
        ResultMatcher rm1 =
                jsonPath(
                        "$.results.*.primaryAccession",
                        contains(List.of(TEST_IDS_ARRAY).subList(4, 8).toArray()));
        return List.of(rm1);
    }

    @Override
    protected List<ResultMatcher> getThirdPageResultMatchers() {
        ResultMatcher rm1 =
                jsonPath(
                        "$.results.*.primaryAccession",
                        contains(List.of(TEST_IDS_ARRAY).subList(8, 10).toArray()));
        return List.of(rm1);
    }

    @Override
    protected String[] getErrorMessages() {
        return new String[] {
            "Invalid fields parameter value 'invalid'",
            "Invalid fields parameter value 'invalid1'",
            "The 'download' parameter has invalid format. It should be a boolean true or false.",
            "Accession 'INVALID' has invalid format. It should be a valid UniProtKB accession.",
            "Accession 'INVALID2' has invalid format. It should be a valid UniProtKB accession."
        };
    }

    @Override
    protected String[] getInvalidFacetErrorMessage() {
        return new String[] {
            "Invalid facet name 'invalid_facet1'. Expected value can be [structure_3d, fragment, "
                    + "proteins_with, length, existence, reviewed, annotation_score, model_organism, other_organism, proteome]."
        };
    }

    @Override
    protected String getQueryFilter() {
        return "reviewed:true OR reviewed:false";
    }

    @Override
    protected ResultMatcher getSortedIdResultMatcher() {
        return jsonPath("$.results.*.primaryAccession", contains(TEST_IDS_ARRAY_SORTED));
    }

    @Override
    protected ResultMatcher getReverseSortedIdResultMatcher() {
        return jsonPath(
                "$.results.*.primaryAccession",
                contains(
                        Arrays.stream(TEST_IDS_ARRAY_SORTED)
                                .sorted(Comparator.reverseOrder())
                                .collect(Collectors.toList())
                                .toArray()));
    }

    @Override
    protected String getUnmatchedQueryFilter() {
        return "existence:randomvalue";
    }

    @Override
    protected List<SolrCollection> getSolrCollections() {
        return Collections.singletonList(SolrCollection.uniprot);
    }

    @Override
    protected TupleStreamTemplate getTupleStreamTemplate() {
        return tupleStreamTemplate;
    }

    @Override
    protected FacetTupleStreamTemplate getFacetTupleStreamTemplate() {
        return facetTupleStreamTemplate;
    }

    @Override
    protected String[] getIdLengthErrorMessage() {
        return new String[] {"Only '1000' accessions are allowed in each request."};
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.UNIPROTKB;
    }
}
