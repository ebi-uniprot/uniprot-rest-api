package org.uniprot.api.idmapping.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.idmapping.IDMappingREST;
import org.uniprot.api.idmapping.controller.request.IdMappingJobRequest;
import org.uniprot.api.idmapping.controller.response.JobStatus;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.idmapping.model.IdMappingResult;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.api.idmapping.service.HashGenerator;
import org.uniprot.api.idmapping.service.IdMappingJobCacheService;
import org.uniprot.api.rest.controller.AbstractStreamControllerIT;
import org.uniprot.core.cv.xdb.UniProtDatabaseDetail;
import org.uniprot.core.gene.Gene;
import org.uniprot.core.json.parser.taxonomy.TaxonomyLineageTest;
import org.uniprot.core.json.parser.uniprot.FeatureTest;
import org.uniprot.core.json.parser.uniprot.GeneLocationTest;
import org.uniprot.core.json.parser.uniprot.OrganimHostTest;
import org.uniprot.core.json.parser.uniprot.UniProtKBCrossReferenceTest;
import org.uniprot.core.json.parser.uniprot.comment.AlternativeProductsCommentTest;
import org.uniprot.core.json.parser.uniprot.comment.BPCPCommentTest;
import org.uniprot.core.json.parser.uniprot.comment.CatalyticActivityCommentTest;
import org.uniprot.core.json.parser.uniprot.comment.CofactorCommentTest;
import org.uniprot.core.json.parser.uniprot.comment.DiseaseCommentTest;
import org.uniprot.core.json.parser.uniprot.comment.FreeTextCommentTest;
import org.uniprot.core.json.parser.uniprot.comment.InteractionCommentTest;
import org.uniprot.core.json.parser.uniprot.comment.MassSpectrometryCommentTest;
import org.uniprot.core.json.parser.uniprot.comment.RnaEditingCommentTest;
import org.uniprot.core.json.parser.uniprot.comment.SequenceCautionCommentTest;
import org.uniprot.core.json.parser.uniprot.comment.SubcellularLocationCommentTest;
import org.uniprot.core.json.parser.uniprot.comment.WebResourceCommentTest;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.uniprotkb.UniProtKBEntryType;
import org.uniprot.core.uniprotkb.comment.Comment;
import org.uniprot.core.uniprotkb.comment.CommentType;
import org.uniprot.core.uniprotkb.comment.FreeTextComment;
import org.uniprot.core.uniprotkb.comment.impl.FreeTextCommentBuilder;
import org.uniprot.core.uniprotkb.comment.impl.FreeTextCommentImpl;
import org.uniprot.core.uniprotkb.evidence.impl.EvidencedValueBuilder;
import org.uniprot.core.uniprotkb.feature.UniProtKBFeature;
import org.uniprot.core.uniprotkb.feature.UniprotKBFeatureType;
import org.uniprot.core.uniprotkb.impl.GeneBuilder;
import org.uniprot.core.uniprotkb.impl.GeneNameBuilder;
import org.uniprot.core.uniprotkb.impl.ORFNameBuilder;
import org.uniprot.core.uniprotkb.impl.OrderedLocusNameBuilder;
import org.uniprot.core.uniprotkb.impl.UniProtKBEntryBuilder;
import org.uniprot.core.uniprotkb.xdb.UniProtKBCrossReference;
import org.uniprot.cv.chebi.ChebiRepo;
import org.uniprot.cv.ec.ECRepo;
import org.uniprot.cv.go.GORepo;
import org.uniprot.cv.xdb.UniProtDatabaseTypes;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.indexer.uniprot.mockers.PathwayRepoMocker;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.indexer.uniprot.mockers.UniProtEntryMocker;
import org.uniprot.store.indexer.uniprotkb.converter.UniProtEntryConverter;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.uniprot.UniProtDocument;

/**
 * @author sahmad
 * @created 18/02/2021
 */
@ActiveProfiles(profiles = "offline")
@ContextConfiguration(classes = {DataStoreTestConfig.class, IDMappingREST.class})
@WebMvcTest(UniProtKBIdMappingResultsController.class)
@AutoConfigureWebClient
@ExtendWith(value = {SpringExtension.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UniProtKBIdMappingControllerIT extends AbstractStreamControllerIT {
    private static final String UNIPROTKB_ID_MAPPING_SEARCH =
            "/uniprotkb/idmapping/results/{jobId}";

    @Autowired private UniProtStoreClient<UniProtKBEntry> storeClient;
    @Autowired private IdMappingJobCacheService idMappingJobCacheService;
    private final HashGenerator hashGenerator = new HashGenerator();

    @Qualifier("uniproKBfacetTupleStreamTemplate")
    @Autowired
    private FacetTupleStreamTemplate facetTupleStreamTemplate;

    @Qualifier("uniProtKBTupleStreamTemplate")
    @Autowired
    private TupleStreamTemplate tupleStreamTemplate;

    @Autowired private MockMvc mockMvc;
    private final UniProtEntryConverter documentConverter =
            new UniProtEntryConverter(
                    TaxonomyRepoMocker.getTaxonomyRepo(),
                    mock(GORepo.class),
                    PathwayRepoMocker.getPathwayRepo(),
                    mock(ChebiRepo.class),
                    mock(ECRepo.class),
                    new HashMap<>());

    @Override
    protected List<SolrCollection> getSolrCollections() {
        return List.of(SolrCollection.uniprot);
    }

    @Override
    protected TupleStreamTemplate getTupleStreamTemplate() {
        return tupleStreamTemplate;
    }

    @Override
    protected FacetTupleStreamTemplate getFacetTupleStreamTemplate() {
        return facetTupleStreamTemplate;
    }

    private static final UniProtKBEntry TEMPLATE_ENTRY =
            UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);

    @BeforeAll
    void saveEntriesStore() throws Exception {
        for (int i = 1; i <= 20; i++) {
            UniProtKBEntryBuilder entryBuilder = UniProtKBEntryBuilder.from(TEMPLATE_ENTRY);
            String acc = String.format("Q%05d", i);
            entryBuilder.primaryAccession(acc);
            if (i % 2 == 0) {
                entryBuilder.entryType(UniProtKBEntryType.SWISSPROT);
            } else {
                entryBuilder.entryType(UniProtKBEntryType.TREMBL);
            }

            List<Comment> comments = createAllComments();
            entryBuilder.extraAttributesAdd(
                    UniProtKBEntryBuilder.UNIPARC_ID_ATTRIB, "UP1234567890");
            entryBuilder.lineagesAdd(TaxonomyLineageTest.getCompleteTaxonomyLineage());
            entryBuilder.geneLocationsAdd(GeneLocationTest.getGeneLocation());
            Gene gene =
                    new GeneBuilder()
                            .geneName(new GeneNameBuilder().value("gene " + i).build())
                            .orderedLocusNamesAdd(
                                    new OrderedLocusNameBuilder().value("gene " + i).build())
                            .orfNamesAdd(new ORFNameBuilder().value("gene " + i).build())
                            .build();
            entryBuilder.genesAdd(gene);
            entryBuilder.organismHostsAdd(OrganimHostTest.getOrganismHost());
            UniProtKBEntry uniProtKBEntry = entryBuilder.build();
            uniProtKBEntry.getComments().addAll(comments);

            uniProtKBEntry.getUniProtKBCrossReferences().addAll(createDatabases());
            uniProtKBEntry.getFeatures().addAll(getFeatures());

            storeClient.saveEntry(uniProtKBEntry);

            UniProtDocument doc = documentConverter.convert(uniProtKBEntry);
            cloudSolrClient.addBean(SolrCollection.uniprot.name(), doc);
            cloudSolrClient.commit(SolrCollection.uniprot.name());
        }
    }

    @Test
    void testUniProtKBToUniProtKBMapping() throws Exception {
        // when
        IdMappingJob job = createAndPutJobInCache("ACC", "ACC", "Q00001,Q00002");
        ResultActions response =
                mockMvc.perform(
                        get(UNIPROTKB_ID_MAPPING_SEARCH, job.getJobId())
                                .header(ACCEPT, MediaType.APPLICATION_JSON));
        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", Matchers.is(2)))
                .andExpect(jsonPath("$.results.*.from", contains("Q00001", "Q00002")))
                .andExpect(
                        jsonPath("$.results.*.to.primaryAccession", contains("Q00001", "Q00002")));
    }

    @Test
    void testUniProtKBToUniProtKBMappingOnePage() throws Exception {
        // when
        String ids =
                IntStream.rangeClosed(1, 20)
                        .mapToObj(i -> String.format("Q%05d", i))
                        .collect(Collectors.joining(","));
        Integer defaultPageSize = 5;
        IdMappingJob job = createAndPutJobInCache("ACC", "ACC", ids);

        ResultActions response =
                mockMvc.perform(
                        get(UNIPROTKB_ID_MAPPING_SEARCH, job.getJobId())
                                .header(ACCEPT, MediaType.APPLICATION_JSON));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", Matchers.is(defaultPageSize)))
                .andExpect(
                        jsonPath(
                                "$.results.*.from",
                                contains("Q00001", "Q00002", "Q00003", "Q00004", "Q00005")))
                .andExpect(
                        jsonPath(
                                "$.results.*.to.primaryAccession",
                                contains("Q00001", "Q00002", "Q00003", "Q00004", "Q00005")));
    }

    @Test
    void testUniProtKBToUniProtKBMappingWithSize() throws Exception {
        // when
        Integer size = 10;
        String ids =
                IntStream.rangeClosed(1, 20)
                        .mapToObj(i -> String.format("Q%05d", i))
                        .collect(Collectors.joining(","));

        IdMappingJob job = createAndPutJobInCache("ACC", "ACC", ids);

        ResultActions response =
                mockMvc.perform(
                        get(UNIPROTKB_ID_MAPPING_SEARCH, job.getJobId())
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("size", String.valueOf(size))
                                .param(
                                        "ids",
                                        "Q00001,Q00002,Q00003,Q00004,Q00005,Q00006,"
                                                + "Q00007,Q00008,Q00009,Q00010,"
                                                + "Q00011,Q00012,Q00013,Q00014,Q00015,Q00016,Q00017,Q00018,Q00019,Q0020"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", Matchers.is(size)))
                .andExpect(
                        jsonPath(
                                "$.results.*.from",
                                contains(
                                        "Q00001", "Q00002", "Q00003", "Q00004", "Q00005", "Q00006",
                                        "Q00007", "Q00008", "Q00009", "Q00010")))
                .andExpect(
                        jsonPath(
                                "$.results.*.to.primaryAccession",
                                contains(
                                        "Q00001", "Q00002", "Q00003", "Q00004", "Q00005", "Q00006",
                                        "Q00007", "Q00008", "Q00009", "Q00010")));
    }

    @Test
    void testUniProtKBToUniProtKBMappingWithSizeAndPagination() throws Exception {
        // when
        Integer size = 10;
        String ids =
                IntStream.rangeClosed(1, 20)
                        .mapToObj(i -> String.format("Q%05d", i))
                        .collect(Collectors.joining(","));

        IdMappingJob job = createAndPutJobInCache("ACC", "ACC", ids);

        ResultActions response =
                mockMvc.perform(
                        get(UNIPROTKB_ID_MAPPING_SEARCH, job.getJobId())
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("size", String.valueOf(size)));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string("X-TotalRecords", "20"))
                .andExpect(jsonPath("$.results.size()", Matchers.is(size)))
                .andExpect(
                        jsonPath(
                                "$.results.*.from",
                                contains(
                                        "Q00001", "Q00002", "Q00003", "Q00004", "Q00005", "Q00006",
                                        "Q00007", "Q00008", "Q00009", "Q00010")))
                .andExpect(
                        jsonPath(
                                "$.results.*.to.primaryAccession",
                                contains(
                                        "Q00001", "Q00002", "Q00003", "Q00004", "Q00005", "Q00006",
                                        "Q00007", "Q00008", "Q00009", "Q00010")));

        String linkHeader = response.andReturn().getResponse().getHeader(HttpHeaders.LINK);
        assertThat(linkHeader, notNullValue());
        String cursor = linkHeader.split("\\?")[1].split("&")[0].split("=")[1];

        // when 2nd page
        response =
                mockMvc.perform(
                        get(UNIPROTKB_ID_MAPPING_SEARCH, job.getJobId())
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("size", String.valueOf(size))
                                .param("cursor", cursor));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string("X-TotalRecords", "20"))
                .andExpect(header().string(HttpHeaders.LINK, nullValue()))
                .andExpect(jsonPath("$.results.size()", Matchers.is(size)))
                .andExpect(
                        jsonPath(
                                "$.results.*.from",
                                contains(
                                        "Q00011", "Q00012", "Q00013", "Q00014", "Q00015", "Q00016",
                                        "Q00017", "Q00018", "Q00019", "Q00020")))
                .andExpect(
                        jsonPath(
                                "$.results.*.to.primaryAccession",
                                contains(
                                        "Q00011", "Q00012", "Q00013", "Q00014", "Q00015", "Q00016",
                                        "Q00017", "Q00018", "Q00019", "Q00020")));
    }

    @Test
    void testUniProtKBToUniProtKBMappingWithZeroSize() throws Exception {
        // when
        IdMappingJob job = createAndPutJobInCache("ACC", "ACC", "Q00001,Q00002");
        ResultActions response =
                mockMvc.perform(
                        get(UNIPROTKB_ID_MAPPING_SEARCH, job.getJobId())
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("size", "0"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", Matchers.is(0)));
    }

    @Test
    void testUniProtKBToUniProtKBMappingWithNegativeSize() throws Exception {
        // when
        IdMappingJob job = createAndPutJobInCache("ACC", "ACC", "Q00001,Q00002");
        ResultActions response =
                mockMvc.perform(
                        get(UNIPROTKB_ID_MAPPING_SEARCH, job.getJobId())
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("size", "-1"));
        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains("'size' must be greater than or equal to 0")));
    }

    @Test
    void testUniProtKBToUniProtKBMappingWithMoreThan500Size() throws Exception {
        // when
        IdMappingJob job = createAndPutJobInCache("ACC", "ACC", "Q00001,Q00002");
        ResultActions response =
                mockMvc.perform(
                        get(UNIPROTKB_ID_MAPPING_SEARCH, job.getJobId())
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("size", "600"));
        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains("'size' must be less than or equal to 500")));
    }

    // TODO: Leo improve
    @Test
    void testUniProtKBToUniProtKBMappingWithFacet() throws Exception {
        // when
        IdMappingJob job = createAndPutJobInCache("ACC", "ACC", "Q00001,Q00002");
        ResultActions response =
                mockMvc.perform(
                        get(UNIPROTKB_ID_MAPPING_SEARCH, job.getJobId())
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("facets", "proteins_with,reviewed"));
        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", Matchers.is(2)))
                .andExpect(jsonPath("$.results.*.from", contains("Q00001", "Q00002")))
                .andExpect(
                        jsonPath("$.results.*.to.primaryAccession", contains("Q00001", "Q00002")));
    }

    // TODO: Leo improve (facet and query)
    @Test
    void testUniProtKBToUniProtKBMappingWithFacetFilter() throws Exception {
        // when
        IdMappingJob job = createAndPutJobInCache("ACC", "ACC", "Q00001,Q00002");
        ResultActions response =
                mockMvc.perform(
                        get(UNIPROTKB_ID_MAPPING_SEARCH, job.getJobId())
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("facets", "proteins_with,reviewed")
                                .param("query", "reviewed:true"));
        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", Matchers.is(1)))
                .andExpect(jsonPath("$.results.*.from", contains("Q00002")))
                .andExpect(jsonPath("$.results.*.to.primaryAccession", contains("Q00002")));
    }

    @Test
    void testCanSortMultipleFieldsWithSuccess() throws Exception {
        // when
        IdMappingJob job = createAndPutJobInCache("ACC", "ACC", "Q00001,Q00002");
        ResultActions response =
                mockMvc.perform(
                        get(UNIPROTKB_ID_MAPPING_SEARCH, job.getJobId())
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("facets", "proteins_with,reviewed")
                                .param("sort", "gene desc , accession asc"));
        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", Matchers.is(2)))
                .andExpect(jsonPath("$.results.*.from", contains("Q00002", "Q00001")))
                .andExpect(
                        jsonPath("$.results.*.to.primaryAccession", contains("Q00002", "Q00001")));
    }

    // TODO: test for all queries
    // TODO: all Facets
    // TODO: Test just with JobId (default)
    @ParameterizedTest(name = "[{index}] sortFieldName {0} desc")
    @MethodSource("getAllSortFields")
    void testUniProtKBToUniProtKBMappingWithSort(String sortField) throws Exception {
        // when
        IdMappingJob job = createAndPutJobInCache("ACC", "ACC", "Q00001,Q00002");
        ResultActions response =
                mockMvc.perform(
                        get(UNIPROTKB_ID_MAPPING_SEARCH, job.getJobId())
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("facets", "proteins_with,reviewed")
                                .param("sort", sortField + " desc"));
        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", Matchers.is(2)))
                .andExpect(jsonPath("$.results.*.from", contains("Q00002", "Q00001")))
                .andExpect(
                        jsonPath("$.results.*.to.primaryAccession", contains("Q00002", "Q00001")));
    }

    @Test
    void testUniProtKBToUniProtKBMappingWithUnmappedIds() throws Exception {
        // when
        List<String> unmappedIds = List.of("S12345", "T12345");
        IdMappingJob job = createAndPutJobInCache("ACC", "ACC", "Q00005,Q00006");
        job.getIdMappingResult().setUnmappedIds(unmappedIds);

        ResultActions response =
                mockMvc.perform(
                        get(UNIPROTKB_ID_MAPPING_SEARCH, job.getJobId())
                                .header(ACCEPT, MediaType.APPLICATION_JSON));
        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", Matchers.is(2)))
                .andExpect(jsonPath("$.results.*.from", contains("Q00005", "Q00006")))
                .andExpect(
                        jsonPath("$.results.*.to.primaryAccession", contains("Q00005", "Q00006")))
                .andExpect(jsonPath("$.failedIds", contains("S12345", "T12345")));
    }

    @ParameterizedTest(name = "[{index}] return for fieldName {0} and paths: {1}")
    @MethodSource("getAllReturnedFields")
    void searchCanSearchWithAllAvailableReturnedFields(String name, List<String> paths)
            throws Exception {

        assertThat(name, notNullValue());
        assertThat(paths, notNullValue());
        // when
        IdMappingJob job = createAndPutJobInCache("ACC", "ACC", "Q00001");
        ResultActions response =
                mockMvc.perform(
                        get(UNIPROTKB_ID_MAPPING_SEARCH, job.getJobId())
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("fields", name));

        // then
        ResultActions resultActions =
                response.andDo(print())
                        .andExpect(status().is(HttpStatus.OK.value()))
                        .andExpect(
                                header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                        .andExpect(jsonPath("$.results.size()", Matchers.is(1)))
                        .andExpect(jsonPath("$.results.*.from", contains("Q00001")))
                        .andExpect(jsonPath("$.results.*.to.primaryAccession", contains("Q00001")));
        for (String path : paths) {
            String returnFieldValidatePath = "$.results[*].to." + path;
            resultActions.andExpect(jsonPath(returnFieldValidatePath).hasJsonPath());
        }
    }

    protected Stream<Arguments> getAllReturnedFields() {
        return ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.UNIPROTKB)
                .getReturnFields().stream()
                .map(returnField -> Arguments.of(returnField.getName(), returnField.getPaths()));
    }

    private List<Comment> createAllComments() {
        List<Comment> comments = new ArrayList<>();
        comments.add(AlternativeProductsCommentTest.getAlternativeProductsComment());
        comments.add(BPCPCommentTest.getBpcpComment());
        comments.add(CatalyticActivityCommentTest.getCatalyticActivityComment());
        comments.add(CofactorCommentTest.getCofactorComment());
        comments.add(DiseaseCommentTest.getDiseaseComment());
        comments.add(FreeTextCommentTest.getFreeTextComment());
        comments.add(FreeTextCommentTest.getFreeTextComment2());
        comments.add(InteractionCommentTest.getInteractionComment());
        comments.add(MassSpectrometryCommentTest.getMassSpectrometryComment());
        comments.add(RnaEditingCommentTest.getRnaEditingComment());
        comments.add(SequenceCautionCommentTest.getSequenceCautionComment());
        comments.add(SubcellularLocationCommentTest.getSubcellularLocationComment());
        comments.add(WebResourceCommentTest.getWebResourceComment());
        List<Comment> freeTextComments =
                Arrays.stream(CommentType.values())
                        .filter(FreeTextCommentImpl::isFreeTextCommentType)
                        .map(FreeTextCommentTest::getFreeTextComment)
                        .collect(Collectors.toList());

        FreeTextComment similarityFamily =
                new FreeTextCommentBuilder()
                        .commentType(CommentType.SIMILARITY)
                        .textsAdd(
                                new EvidencedValueBuilder()
                                        .value("Belongs to the NSMF family")
                                        .build())
                        .build();
        freeTextComments.add(similarityFamily);

        comments.addAll(freeTextComments);
        return comments;
    }

    private List<UniProtKBCrossReference> createDatabases() {
        List<UniProtKBCrossReference> xrefs =
                UniProtDatabaseTypes.INSTANCE.getAllDbTypes().stream()
                        .map(UniProtDatabaseDetail::getName)
                        .map(UniProtKBCrossReferenceTest::getUniProtDBCrossReference)
                        .collect(Collectors.toList());

        xrefs.add(UniProtKBCrossReferenceTest.getUniProtDBGOCrossReferences("C", "IDA"));
        xrefs.add(UniProtKBCrossReferenceTest.getUniProtDBGOCrossReferences("F", "IDA"));
        xrefs.add(UniProtKBCrossReferenceTest.getUniProtDBGOCrossReferences("P", "IDA"));
        return xrefs;
    }

    private List<UniProtKBFeature> getFeatures() {
        List<UniProtKBFeature> features =
                Arrays.stream(UniprotKBFeatureType.values())
                        .map(FeatureTest::getFeature)
                        .collect(Collectors.toList());
        return features;
    }

    private Stream<Arguments> getAllSortFields() {
        SearchFieldConfig fieldConfig =
                SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIPROTKB);
        return fieldConfig.getSearchFieldItems().stream()
                .map(SearchFieldItem::getFieldName)
                .filter(fieldConfig::correspondingSortFieldExists)
                .map(Arguments::of);
    }

    private IdMappingJob createAndPutJobInCache(String from, String to, String ids)
            throws InvalidKeySpecException, NoSuchAlgorithmException {
        IdMappingJobRequest idMappingRequest = createRequest(from, to, ids);
        String jobId = generateHash(idMappingRequest);
        IdMappingResult idMappingResult = createIdMappingResult(idMappingRequest);
        IdMappingJob job = createJob(jobId, idMappingRequest, idMappingResult, JobStatus.FINISHED);
        if (!this.idMappingJobCacheService.exists(jobId)) {
            this.idMappingJobCacheService.put(jobId, job); // put the finished job in cache
        }
        return job;
    }

    private IdMappingJobRequest createRequest(String from, String to, String ids) {
        IdMappingJobRequest request = new IdMappingJobRequest();
        request.setFrom(from);
        request.setTo(to);
        request.setIds(ids);
        return request;
    }

    private String generateHash(IdMappingJobRequest request)
            throws InvalidKeySpecException, NoSuchAlgorithmException {
        return this.hashGenerator.generateHash(request);
    }

    private IdMappingResult createIdMappingResult(IdMappingJobRequest request) {
        List<IdMappingStringPair> mappedIds =
                Arrays.stream(request.getIds().split(","))
                        .map(String::strip)
                        .map(id -> new IdMappingStringPair(id, id))
                        .collect(Collectors.toList());
        return IdMappingResult.builder().mappedIds(mappedIds).build();
    }

    private IdMappingJob createJob(
            String jobId,
            IdMappingJobRequest request,
            IdMappingResult result,
            JobStatus jobStatus) {
        IdMappingJob.IdMappingJobBuilder builder = IdMappingJob.builder();
        builder.jobId(jobId).jobStatus(jobStatus);
        builder.idMappingRequest(request).idMappingResult(result);
        return builder.build();
    }
}
