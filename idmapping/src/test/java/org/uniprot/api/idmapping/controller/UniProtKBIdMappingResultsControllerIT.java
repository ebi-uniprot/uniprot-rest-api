package org.uniprot.api.idmapping.controller;

import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.mock;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
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
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.idmapping.IdMappingREST;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.rest.respository.facet.impl.UniprotKBFacetConfig;
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
@ContextConfiguration(classes = {DataStoreTestConfig.class, IdMappingREST.class})
@WebMvcTest(UniProtKBIdMappingResultsController.class)
@AutoConfigureWebClient
@ExtendWith(value = {SpringExtension.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UniProtKBIdMappingResultsControllerIT extends AbstractIdMappingResultsControllerIT {
    private static final String UNIPROTKB_ID_MAPPING_RESULT_PATH =
            "/idmapping/uniprotkb/results/{jobId}";

    @Autowired private UniprotKBFacetConfig facetConfig;

    @Autowired private UniProtStoreClient<UniProtKBEntry> storeClient;

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

    @Override
    protected MockMvc getMockMvc() {
        return mockMvc;
    }

    @Override
    protected String getIdMappingResultPath() {
        return UNIPROTKB_ID_MAPPING_RESULT_PATH;
    }

    @Override
    protected IdMappingJob createAndPutJobInCache() throws Exception {
        String ids =
                IntStream.rangeClosed(1, 20)
                        .mapToObj(i -> String.format("Q%05d", i))
                        .collect(Collectors.joining(","));
        return createAndPutJobInCache("ACC", "ACC", ids);
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.UNIPROTKB;
    }

    @Override
    protected FacetConfig getFacetConfig() {
        return facetConfig;
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
            doc.otherOrganism="otherValue";
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
                        get(UNIPROTKB_ID_MAPPING_RESULT_PATH, job.getJobId())
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

    // TODO: Leo improve (facet and query)
    @Test
    void testUniProtKBToUniProtKBMappingWithFacetFilter() throws Exception {
        // when
        IdMappingJob job = createAndPutJobInCache("ACC", "ACC", "Q00001,Q00002");
        ResultActions response =
                mockMvc.perform(
                        get(UNIPROTKB_ID_MAPPING_RESULT_PATH, job.getJobId())
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
                        get(UNIPROTKB_ID_MAPPING_RESULT_PATH, job.getJobId())
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

    @Test
    void testUniProtKBToUniProtKBMappingWithUnmappedIds() throws Exception {
        // when
        List<String> unmappedIds = List.of("S12345", "T12345");
        IdMappingJob job = createAndPutJobInCache("ACC", "ACC", "Q00005,Q00006");
        job.getIdMappingResult().setUnmappedIds(unmappedIds);

        ResultActions response =
                mockMvc.perform(
                        get(UNIPROTKB_ID_MAPPING_RESULT_PATH, job.getJobId())
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

    // TODO: remove duplicated code with UniprotIT
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
}
