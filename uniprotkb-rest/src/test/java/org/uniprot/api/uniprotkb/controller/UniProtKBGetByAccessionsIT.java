package org.uniprot.api.uniprotkb.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.uniprot.api.rest.output.UniProtMediaType.FASTA_MEDIA_TYPE;
import static org.uniprot.api.rest.output.UniProtMediaType.FASTA_MEDIA_TYPE_VALUE;
import static org.uniprot.store.search.field.validator.FieldRegexConstants.UNIPROTKB_ACCESSION_SEQUENCE_RANGE_REGEX;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.solr.client.solrj.SolrServerException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.rest.controller.AbstractGetByIdsControllerIT;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.respository.facet.impl.UniProtKBFacetConfig;
import org.uniprot.api.uniprotkb.UniProtKBREST;
import org.uniprot.api.uniprotkb.common.repository.UniProtKBDataStoreTestConfig;
import org.uniprot.core.fasta.UniProtKBFasta;
import org.uniprot.core.gene.Gene;
import org.uniprot.core.gene.GeneName;
import org.uniprot.core.parser.fasta.uniprot.UniProtKBFastaParser;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.uniprotkb.UniProtKBEntryType;
import org.uniprot.core.uniprotkb.impl.GeneBuilder;
import org.uniprot.core.uniprotkb.impl.GeneNameBuilder;
import org.uniprot.core.uniprotkb.impl.UniProtKBEntryBuilder;
import org.uniprot.cv.taxonomy.TaxonomyRepo;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.indexer.uniprot.mockers.UniProtEntryMocker;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.uniprot.UniProtDocument;
import org.uniprot.store.spark.indexer.uniprot.converter.UniProtEntryConverter;

/**
 * @author lgonzales
 * @since 2019-07-10
 */
@ContextConfiguration(classes = {UniProtKBDataStoreTestConfig.class, UniProtKBREST.class})
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
            new UniProtEntryConverter(new HashMap<>(), new HashMap<>());

    private final TaxonomyRepo taxonomyRepo = TaxonomyRepoMocker.getTaxonomyRepo();

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

    private static final String TEST_IDS_SEQ_RANGE =
            "P00003[10-20],P00007,P00003,P00001,P00003[20-40],P00003[1-5],P00002";
    private static final String MISSING_ID1 = "Q00001";
    private static final String MISSING_ID2 = "Q00002";

    @Autowired private UniProtStoreClient<UniProtKBEntry> uniProtKBStoreClient;

    @Autowired private FacetTupleStreamTemplate facetTupleStreamTemplate;
    @Autowired private TupleStreamTemplate tupleStreamTemplate;

    @Autowired private MockMvc mockMvc;

    @Autowired private UniProtKBFacetConfig facetConfig;

    @BeforeAll
    void saveEntriesInSolrAndStore() throws Exception {
        char prefix = 'z';
        for (int i = 1; i <= 30; i++) {
            UniProtKBEntryBuilder entryBuilder = UniProtKBEntryBuilder.from(TEMPLATE_ENTRY);
            String acc = String.format("P%05d", i);
            System.out.print(acc + ",");
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
            saveEntry(uniProtKBEntry);
            prefix--;
        }
        UniProtKBEntry uniProtKBEntry =
                UniProtKBEntryBuilder.from(UniProtEntryMocker.create(UniProtEntryMocker.Type.TR))
                        .build();
        saveEntry(uniProtKBEntry);

        uniProtKBEntry =
                UniProtKBEntryBuilder.from(
                                UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_ISOFORM))
                        .build();
        saveEntry(uniProtKBEntry);

        uniProtKBEntry =
                UniProtKBEntryBuilder.from(
                                UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL))
                        .build();
        saveEntry(uniProtKBEntry);

        cloudSolrClient.commit(SolrCollection.uniprot.name());
    }

    private void saveEntry(UniProtKBEntry uniProtKBEntry) throws IOException, SolrServerException {
        UniProtDocument document = documentConverter.convert(uniProtKBEntry);
        UniProtKBEntryConvertITUtils.aggregateTaxonomyDataToDocument(taxonomyRepo, document);

        cloudSolrClient.addBean(SolrCollection.uniprot.name(), document);
        uniProtKBStoreClient.saveEntry(uniProtKBEntry);
    }

    @Test
    void getByIdsSortByProteinNameWithCorrectValuesSuccess() throws Exception {
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(getGetByIdsPath())
                                        .header(
                                                org.apache.http.HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON)
                                        .param(getRequestParamName(), getCommaSeparatedIds())
                                        .param("sort", "gene asc")
                                        .param("size", "10"));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(10)))
                .andExpect(getReverseSortedIdResultMatcher())
                .andExpect(MockMvcResultMatchers.jsonPath("$.facets").doesNotExist());
    }

    @Test
    void getByIdsMixedIdsWithIsoformAndNonIsoformEntriesSuccess() throws Exception {
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(getGetByIdsPath())
                                        .header(
                                                org.apache.http.HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON)
                                        .param(getRequestParamName(), "p21802-2,p00003,p21802")
                                        .param("size", "10"));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(3)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession",
                                contains("P00003", "P21802", "P21802-2")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.facets").doesNotExist());
    }

    @Test
    @Tag("TRM-28917")
    void getByIdsCanonicalIsoformForTremblEntriesSuccess() throws Exception {
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(getGetByIdsPath())
                                        .header(
                                                org.apache.http.HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON)
                                        .param(getRequestParamName(), "F1Q0X3-1")
                                        .param("size", "10"));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(1)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results[0].primaryAccession", is("F1Q0X3")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.facets").doesNotExist());
    }

    @Test
    void getByIdsWithSequenceRangeSuccess() throws Exception {
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getGetByIdsPath())
                                        .header(ACCEPT, FASTA_MEDIA_TYPE)
                                        .param(getRequestParamName(), TEST_IDS_SEQ_RANGE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().doesNotExist("Content-Disposition"))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_TYPE,
                                        UniProtMediaType.FASTA_MEDIA_TYPE_VALUE));
        String fastaResponse = response.andReturn().getResponse().getContentAsString();
        verifyFastaResponse(fastaResponse);
    }

    @Test
    void getByIdsWithSequenceRangeSortedByAccessionSuccess() throws Exception {
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getGetByIdsPath())
                                        .param(getRequestParamName(), TEST_IDS_SEQ_RANGE)
                                        .param("sort", "accession asc")
                                        .param("query", "reviewed:true")
                                        .header(ACCEPT, FASTA_MEDIA_TYPE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, FASTA_MEDIA_TYPE_VALUE))
                .andExpect(
                        content().string(containsString("sort not supported with sequence range")))
                .andExpect(
                        content()
                                .string(containsString("query not supported with sequence range")));
    }

    @Test
    void downloadByIdsWithSequenceRangeSuccess() throws Exception {
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getGetByIdsPath())
                                        .param("download", "true")
                                        .header(ACCEPT, FASTA_MEDIA_TYPE)
                                        .param(getRequestParamName(), TEST_IDS_SEQ_RANGE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_TYPE,
                                        UniProtMediaType.FASTA_MEDIA_TYPE_VALUE));
        String fastaResponse = response.andReturn().getResponse().getContentAsString();
        verifyFastaResponse(fastaResponse);
    }

    @ParameterizedTest
    @MethodSource("provideInvalidRequests")
    void getByIdsWithSequenceRangeWithInvalidRequests(
            String accessions, String format, List<ResultMatcher> matchers) throws Exception {
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getGetByIdsPath())
                                        .header(ACCEPT, format)
                                        .param(getRequestParamName(), accessions));

        // then
        ResultActions resultActions =
                response.andDo(log())
                        .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, format));
        for (ResultMatcher matcher : matchers) {
            resultActions.andExpect(matcher);
        }
    }

    @Test
    void getByIdsMixedIdsWithIsoformAndNonIsoformEntriesMoreThanPageSizeSuccess() throws Exception {
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(getGetByIdsPath())
                                        .header(
                                                org.apache.http.HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON)
                                        .param(
                                                getRequestParamName(),
                                                "p21802-2,p21802,P00001,P00002,P00003,P00004,P00005,P00006,P00007,"
                                                        + "P00008,P00009,P00010,P00011,P00012,P00013,P00014,P00015,P00016,P00017,"
                                                        + "P00018,P00019,P00020,P00021,P00022,P00023,P00024,P00025,P00026,P00027,"
                                                        + "P00028,P00029,P00030"));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(32)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession",
                                contains(
                                        "P00001",
                                        "P00002",
                                        "P00003",
                                        "P00004",
                                        "P00005",
                                        "P00006",
                                        "P00007",
                                        "P00008",
                                        "P00009",
                                        "P00010",
                                        "P00011",
                                        "P00012",
                                        "P00013",
                                        "P00014",
                                        "P00015",
                                        "P00016",
                                        "P00017",
                                        "P00018",
                                        "P00019",
                                        "P00020",
                                        "P00021",
                                        "P00022",
                                        "P00023",
                                        "P00024",
                                        "P00025",
                                        "P00026",
                                        "P00027",
                                        "P00028",
                                        "P00029",
                                        "P00030",
                                        "P21802",
                                        "P21802-2")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.facets").doesNotExist());
    }

    private void verifyFastaResponse(String fastaResponse) {
        List<UniProtKBFasta> uniProtKBFastas = getUniProtKBFastas(fastaResponse);
        Assertions.assertEquals(TEST_IDS_SEQ_RANGE.split(",").length, uniProtKBFastas.size());
        List<String> passedAccessions =
                Arrays.stream(TEST_IDS_SEQ_RANGE.split(","))
                        .map(
                                id ->
                                        UNIPROTKB_ACCESSION_SEQUENCE_RANGE_REGEX
                                                        .matcher(id)
                                                        .matches()
                                                ? id.substring(0, id.indexOf("["))
                                                : id)
                        .collect(Collectors.toList());
        List<String> returnedIds =
                uniProtKBFastas.stream().map(UniProtKBFasta::getId).collect(Collectors.toList());
        Assertions.assertEquals(passedAccessions, returnedIds);

        String[] ids = TEST_IDS_SEQ_RANGE.split(",");
        for (int i = 0; i < uniProtKBFastas.size(); i++) {
            UniProtKBFasta fasta = uniProtKBFastas.get(i);
            String id = ids[i];
            if (UNIPROTKB_ACCESSION_SEQUENCE_RANGE_REGEX.matcher(id).matches()) {
                String[] range = id.substring(id.indexOf('[') + 1, id.indexOf(']')).split("-");
                int sequenceLength = Integer.parseInt(range[1]) - Integer.parseInt(range[0]) + 1;
                Assertions.assertEquals(sequenceLength, fasta.getSequence().getLength());
                Assertions.assertEquals(range[0] + "-" + range[1], fasta.getSequenceRange());
            }
        }
    }

    private List<UniProtKBFasta> getUniProtKBFastas(String fastaResponse) {
        String[] fastaEntries = fastaResponse.split("\n>");
        List<UniProtKBFasta> uniProtKBFastas =
                Arrays.stream(fastaEntries)
                        .map(fasta -> fasta.startsWith(">") ? fasta : ">" + fasta)
                        .map(UniProtKBFastaParser::fromFastaString)
                        .collect(Collectors.toList());
        return uniProtKBFastas;
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
        ResultMatcher rm1 =
                MockMvcResultMatchers.jsonPath(
                        "$.results.*.primaryAccession", contains(TEST_IDS_ARRAY));
        ResultMatcher rm2 =
                MockMvcResultMatchers.jsonPath(
                        "$.results[0].entryType", equalTo("UniProtKB unreviewed (TrEMBL)"));
        ResultMatcher rm3 =
                MockMvcResultMatchers.jsonPath("$.results[0].uniProtkbId", equalTo("FGFR2_HUMAN"));
        return List.of(rm1, rm2, rm3);
    }

    @Override
    protected List<ResultMatcher> getFacetsResultMatchers() {
        ResultMatcher rm1 = MockMvcResultMatchers.jsonPath("$.facets.size()", is(9));
        ResultMatcher rm2 =
                MockMvcResultMatchers.jsonPath(
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
                .map(id -> MockMvcResultMatchers.content().string(containsString(id)))
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
        ResultMatcher rm1 =
                MockMvcResultMatchers.jsonPath(
                        "$.results.*.primaryAccession", contains(TEST_IDS_ARRAY));
        ResultMatcher rm2 = MockMvcResultMatchers.jsonPath("$.results.*.organism").exists();
        ResultMatcher rm3 = MockMvcResultMatchers.jsonPath("$.results.*.genes").exists();
        ResultMatcher rm4 = MockMvcResultMatchers.jsonPath("$.results.*.sequence").doesNotExist();
        ResultMatcher rm5 = MockMvcResultMatchers.jsonPath("$.results.*.comments").doesNotExist();
        ResultMatcher rm6 =
                MockMvcResultMatchers.jsonPath(
                        "$.results[0].organism.scientificName", equalTo("Homo sapiens"));
        ResultMatcher rm7 =
                MockMvcResultMatchers.jsonPath(
                        "$.results[0].organism.commonName", equalTo("Human"));
        ResultMatcher rm8 =
                MockMvcResultMatchers.jsonPath("$.results[0].organism.taxonId", equalTo(9606));
        ResultMatcher rm9 =
                MockMvcResultMatchers.jsonPath(
                        "$.results[0].organism.lineage",
                        equalTo(TEMPLATE_ENTRY.getOrganism().getLineages()));
        return List.of(rm1, rm2, rm3, rm4, rm5, rm6, rm7, rm8, rm9);
    }

    @Override
    protected List<ResultMatcher> getFirstPageResultMatchers() {
        ResultMatcher rm1 =
                MockMvcResultMatchers.jsonPath(
                        "$.results.*.primaryAccession",
                        contains(List.of(TEST_IDS_ARRAY).subList(0, 4).toArray()));
        ResultMatcher rm2 = MockMvcResultMatchers.jsonPath("$.facets", iterableWithSize(9));
        ResultMatcher rm3 =
                MockMvcResultMatchers.jsonPath(
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
                MockMvcResultMatchers.jsonPath(
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
        ResultMatcher rm5 = MockMvcResultMatchers.jsonPath("$.facets.*.values").exists();
        ResultMatcher rm6 = MockMvcResultMatchers.jsonPath("$.facets.*.values").isArray();
        return List.of(rm1, rm2, rm3, rm4, rm5, rm6);
    }

    @Override
    protected List<ResultMatcher> getSecondPageResultMatchers() {
        ResultMatcher rm1 =
                MockMvcResultMatchers.jsonPath(
                        "$.results.*.primaryAccession",
                        contains(List.of(TEST_IDS_ARRAY).subList(4, 8).toArray()));
        return List.of(rm1);
    }

    @Override
    protected List<ResultMatcher> getThirdPageResultMatchers() {
        ResultMatcher rm1 =
                MockMvcResultMatchers.jsonPath(
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
            "Accession 'INVALID' has invalid format. It should be a valid UniProtKB accession with optional sequence range e.g. P12345[10-20].",
            "Accession 'INVALID2' has invalid format. It should be a valid UniProtKB accession with optional sequence range e.g. P12345[10-20]."
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
        return MockMvcResultMatchers.jsonPath(
                "$.results.*.primaryAccession", contains(TEST_IDS_ARRAY_SORTED));
    }

    @Override
    protected ResultMatcher getReverseSortedIdResultMatcher() {
        return MockMvcResultMatchers.jsonPath(
                "$.results.*.primaryAccession",
                contains(
                        Arrays.stream(TEST_IDS_ARRAY_SORTED)
                                .sorted(Comparator.reverseOrder())
                                .collect(Collectors.toList())
                                .toArray()));
    }

    @Override
    protected String getUnmatchedQueryFilter() {
        return "existence:2";
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

    @Override
    public String getContentDisposition() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy_MM_dd");
        return "uniprotkb_" + now.format(dateTimeFormatter);
    }

    private static Stream<Arguments> provideInvalidRequests() {
        return Stream.of(
                Arguments.of(
                        "P12345[10-20]",
                        APPLICATION_JSON_VALUE,
                        List.of(
                                jsonPath(
                                        "$.messages.*",
                                        contains(
                                                "Invalid content type received, 'application/json'. Expected one of [text/plain;format=fasta].")))),
                Arguments.of(
                        "P12345,P12345[20-10],P12345[10-20]",
                        FASTA_MEDIA_TYPE_VALUE,
                        List.of(
                                content()
                                        .string(
                                                is(
                                                        "Error messages\n"
                                                                + "Invalid sequence range 'P12345[20-10]' in the accession.")))),
                Arguments.of(
                        "Q12345[10-20],P12345[0-10]",
                        FASTA_MEDIA_TYPE_VALUE,
                        List.of(
                                content()
                                        .string(
                                                is(
                                                        "Error messages\n"
                                                                + "Invalid sequence range 'P12345[0-10]' in the accession.")))),
                Arguments.of(
                        "P12345[1-5147483647],Q12345[0-10],Q12345",
                        FASTA_MEDIA_TYPE_VALUE,
                        List.of(
                                content()
                                        .string(
                                                containsString(
                                                        "Invalid sequence range 'P12345[1-5147483647]' in the accession."))),
                        content()
                                .string(
                                        containsString(
                                                "Invalid sequence range 'Q12345[0-10]' in the accession."))),
                Arguments.of(
                        "Q12345,P12345.3[1-10]",
                        FASTA_MEDIA_TYPE_VALUE,
                        List.of(
                                content()
                                        .string(
                                                is(
                                                        "Error messages\n"
                                                                + "Accession 'P12345.3[1-10]' has invalid format. It should be a valid UniProtKB accession with optional sequence range e.g. P12345[10-20].")))),
                Arguments.of(
                        "P12345[1-10].3,Q12345",
                        FASTA_MEDIA_TYPE_VALUE,
                        List.of(
                                content()
                                        .string(
                                                is(
                                                        "Error messages\n"
                                                                + "Accession 'P12345[1-10].3' has invalid format. It should be a valid UniProtKB accession with optional sequence range e.g. P12345[10-20].")))));
    }
}
