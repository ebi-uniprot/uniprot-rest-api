package org.uniprot.api.uniprotkb.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.uniprot.api.uniprotkb.controller.UniProtKBController.UNIPROTKB_RESOURCE;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Triple;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.common.repository.stream.store.uniprotkb.TaxonomyLineageRepository;
import org.uniprot.api.rest.controller.AbstractSearchWithSuggestionsControllerIT;
import org.uniprot.api.rest.controller.SaveScenario;
import org.uniprot.api.rest.controller.param.ContentTypeParam;
import org.uniprot.api.rest.controller.param.SearchContentTypeParam;
import org.uniprot.api.rest.controller.param.SearchParameter;
import org.uniprot.api.rest.controller.param.resolver.AbstractSearchContentTypeParamResolver;
import org.uniprot.api.rest.controller.param.resolver.AbstractSearchParameterResolver;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.converter.ConverterConstants;
import org.uniprot.api.rest.respository.facet.impl.UniProtKBFacetConfig;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.api.uniprotkb.UniProtKBREST;
import org.uniprot.api.uniprotkb.common.repository.UniProtKBDataStoreTestConfig;
import org.uniprot.api.uniprotkb.common.repository.search.UniprotQueryRepository;
import org.uniprot.api.uniprotkb.common.repository.store.UniProtKBStoreClient;
import org.uniprot.core.json.parser.taxonomy.TaxonomyEntryTest;
import org.uniprot.core.json.parser.taxonomy.TaxonomyJsonConfig;
import org.uniprot.core.json.parser.uniprot.UniProtKBEntryIT;
import org.uniprot.core.taxonomy.TaxonomyEntry;
import org.uniprot.core.uniprotkb.DeletedReason;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.uniprotkb.UniProtKBEntryType;
import org.uniprot.core.uniprotkb.UniProtKBId;
import org.uniprot.core.uniprotkb.comment.CommentType;
import org.uniprot.core.uniprotkb.description.impl.NameBuilder;
import org.uniprot.core.uniprotkb.description.impl.ProteinDescriptionBuilder;
import org.uniprot.core.uniprotkb.description.impl.ProteinNameBuilder;
import org.uniprot.core.uniprotkb.feature.FeatureCategory;
import org.uniprot.core.uniprotkb.feature.UniprotKBFeatureType;
import org.uniprot.core.uniprotkb.impl.GeneBuilder;
import org.uniprot.core.uniprotkb.impl.GeneNameBuilder;
import org.uniprot.core.uniprotkb.impl.UniProtKBEntryBuilder;
import org.uniprot.cv.taxonomy.TaxonomyRepo;
import org.uniprot.cv.xdb.UniProtDatabaseTypes;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.datastore.voldemort.uniprot.VoldemortInMemoryUniprotEntryStore;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.indexer.uniprot.inactiveentry.InactiveUniProtEntry;
import org.uniprot.store.indexer.uniprot.mockers.InactiveEntryMocker;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.indexer.uniprot.mockers.UniProtEntryMocker;
import org.uniprot.store.indexer.uniprotkb.processor.InactiveEntryConverter;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.taxonomy.TaxonomyDocument;
import org.uniprot.store.search.document.uniprot.UniProtDocument;
import org.uniprot.store.search.domain.EvidenceGroup;
import org.uniprot.store.search.domain.EvidenceItem;
import org.uniprot.store.search.domain.impl.GoEvidences;
import org.uniprot.store.spark.indexer.uniprot.converter.UniProtEntryConverter;

import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.extern.slf4j.Slf4j;

@ContextConfiguration(
        classes = {
            UniProtKBDataStoreTestConfig.class,
            UniProtKBREST.class,
            ErrorHandlerConfig.class
        })
@ActiveProfiles(profiles = "offline")
@AutoConfigureWebClient
@WebMvcTest(UniProtKBController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
            UniProtKBSearchControllerIT.UniprotKBSearchParameterResolver.class,
            UniProtKBSearchControllerIT.UniprotKBSearchContentTypeParamResolver.class
        })
@Slf4j
class UniProtKBSearchControllerIT extends AbstractSearchWithSuggestionsControllerIT {

    private static final String SEARCH_RESOURCE = UNIPROTKB_RESOURCE + "/search";

    private static final String ACCESSION_SP_CANONICAL = "P21802";
    private static final String ACCESSION_SP = "Q8DIA7";

    @Autowired private UniprotQueryRepository repository;

    @Autowired private TaxonomyLineageRepository taxRepository;

    @Value("${search.request.converter.defaultRestPageSize:#{null}}")
    private Integer solrBatchSize;

    private UniProtKBStoreClient storeClient;

    @Autowired private UniProtKBFacetConfig facetConfig;

    private static final TaxonomyRepo taxonomyRepo = TaxonomyRepoMocker.getTaxonomyRepo();

    @BeforeAll
    void initUniprotKbDataStore() {
        DataStoreManager dsm = getStoreManager();
        dsm.addDocConverter(
                DataStoreManager.StoreType.UNIPROT, new UniProtEntryConverter(new HashMap<>(), new HashMap<>()));
        dsm.addDocConverter(
                DataStoreManager.StoreType.INACTIVE_UNIPROT, new InactiveEntryConverter());
        dsm.addSolrClient(DataStoreManager.StoreType.INACTIVE_UNIPROT, SolrCollection.uniprot);

        storeClient =
                new UniProtKBStoreClient(
                        VoldemortInMemoryUniprotEntryStore.getInstance("avro-uniprot"));
        dsm.addStore(DataStoreManager.StoreType.UNIPROT, storeClient);

        // Add taxonomy to the repo and and solr template injection..
        dsm.addSolrClient(DataStoreManager.StoreType.TAXONOMY, SolrCollection.taxonomy);
        ReflectionTestUtils.setField(
                taxRepository,
                "solrClient",
                dsm.getSolrClient(DataStoreManager.StoreType.TAXONOMY));
    }

    @AfterEach
    void cleanStore() {
        storeClient.truncate();
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

    // TODO: 10/11/2020
    /*
     * stopwords and phrase queries are known to have problems. Please see:
     * https://mail-archives.apache.org/mod_mbox/lucene-solr-user/202011.mbox/%3CCAA%3DaKsc1oS0FBcijT9tvvShf08QEppEZuAMRY2ZSLtFB%3D5r3Uw%40mail.gmail.com%3E
     *
     * A possible solution would be to pre-process all index/query data going to Solr, and remove its stopwords.
     *
     * If the solution is implemented, then we can eliminate stopwords in the index and save space. We'll then need
     * an integration test below, which tests the full slice:
     * - source text -> document -> index (i.e., indexing data), and
     * - REST app -> Solr query -> index retrieval -> response (i.e., querying data)
     *
     * e.g., index "Molecular cloning and evolution of the genes" and then search for
     *             "Molecular cloning and evolution of the genes".
     * (the two adjacent stopwords was causing problems when configuring Solr directly to handle this using the
     * stopword filter factory and removeduplicatesfilterfactory)
     *
     */

    @Test
    void searchInGreekChars() throws Exception {
        // given
        UniProtKBEntry entry =
                UniProtKBEntryBuilder.from(UniProtEntryMocker.create("P12345"))
                        .genesAdd(
                                new GeneBuilder()
                                        .geneName(new GeneNameBuilder().value("genealpha").build())
                                        .build())
                        .build();
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(
                                                SEARCH_RESOURCE + "?query=gene:gene" + "\u03B1")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(1)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession", contains("P12345")));
    }

    @Test
    void searchInvalidIncludeIsoformParameterValue() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(
                                                SEARCH_RESOURCE
                                                        + "?query=accession:P21802&includeIsoform=invalid")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.messages.*",
                                contains(
                                        "Invalid includeIsoform parameter value. Expected true or false")));
    }

    @Test
    void searchInvalidZeroLength() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(
                                                SEARCH_RESOURCE + "?query=(length:[0 TO 1000])")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.messages.*",
                                contains(
                                        "The 'length' filter value '[0 TO 1000]' is invalid. The min value for the lower is 1.")));
    }

    @Test
    void searchLengthMinOne_success() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(
                                                SEARCH_RESOURCE + "?query=(length:[1 TO 1000])")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(1)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"HGNC:3689\"", "FGFR2", "3689", "\"hgnc-HGNC:3689\"", "hgnc-3689"})
    void searchWithHGNCIdAndProperties(String xref) throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        String acc = entry.getPrimaryAccession().getValue();
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(SEARCH_RESOURCE + "?query=xref:" + xref)
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(1)));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "embl-joined",
                "embl-JOINED",
                "joined",
                "embl-genomic_dna",
                "embl-Genomic_DNA",
                "JOINED",
                "Genomic_DNA",
                "genomic_dna"
            })
    void searchWithEMBLXrefStatus(String xref) throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        String acc = entry.getPrimaryAccession().getValue();
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(SEARCH_RESOURCE + "?query=xref:" + xref)
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(1)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.results[0].primaryAccession", is(acc)));
    }

    @Test
    void searchWithPIRSFXrefEntryName() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        String acc = entry.getPrimaryAccession().getValue();
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(
                                                SEARCH_RESOURCE + "?query=xref:pirsf-FGFR")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(1)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results[0].primaryAccession", is("P21802")));
    }

    @Test
    void searchWithForwardSlash() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        String acc = entry.getPrimaryAccession().getValue();
        UniProtKBEntryBuilder entryBuilder = UniProtKBEntryBuilder.from(entry);
        entryBuilder.proteinDescription(
                new ProteinDescriptionBuilder()
                        .recommendedName(
                                new ProteinNameBuilder()
                                        .fullName(
                                                new NameBuilder()
                                                        .value(
                                                                "Serine/arginine repetitive matrix protein 2")
                                                        .build())
                                        .build())
                        .build());
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entryBuilder.build());

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_ISOFORM);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL_ISOFORM);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(
                                                SEARCH_RESOURCE
                                                        + "?query=(Serine/arginine repetitive matrix protein 2)")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(1)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession", contains(acc)));
    }

    @Test
    void phraseSearchWithForwardSlash() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        String acc = entry.getPrimaryAccession().getValue();
        UniProtKBEntryBuilder entryBuilder = UniProtKBEntryBuilder.from(entry);
        entryBuilder.proteinDescription(
                new ProteinDescriptionBuilder()
                        .recommendedName(
                                new ProteinNameBuilder()
                                        .fullName(
                                                new NameBuilder()
                                                        .value(
                                                                "Serine/arginine repetitive matrix protein 2")
                                                        .build())
                                        .build())
                        .build());
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entryBuilder.build());

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_ISOFORM);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL_ISOFORM);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(
                                                SEARCH_RESOURCE
                                                        + "?query=(\"Serine/arginine repetitive matrix protein 2\")")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(1)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession", contains(acc)));
    }

    @Test
    void searchSecondaryAccession() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_ISOFORM);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL_ISOFORM);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(
                                                SEARCH_RESOURCE
                                                        + "?query=sec_acc:B4DFC2&fields=accession,gene_primary")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(1)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession", contains("P21802")));
    }

    @Test
    void searchCanonicalOnly() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL_ISOFORM);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_ISOFORM);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(
                                                SEARCH_RESOURCE
                                                        + "?query=accession:p21802&fields=accession,gene_primary")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(1)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession", contains("P21802")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession", not("P21802-1")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession", not("P21802-2")));
    }

    @Test
    void searchCanonicalIsoformAccession() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL_ISOFORM);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_ISOFORM);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(
                                                SEARCH_RESOURCE
                                                        + "?query=accession_id:P21802-1&fields=accession,gene_primary")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession", not("P21802")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession", contains("P21802-1")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession", not("P21802-2")));
    }

    @Test
    void searchIncludeCanonicalAndIsoForm() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL_ISOFORM);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_ISOFORM);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(
                                                SEARCH_RESOURCE
                                                        + "?query=gene:FGFR2&fields=accession,gene_primary&includeIsoform=true")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession",
                                containsInAnyOrder("P21802", "P21802-2")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession", not("P21802-1")));
    }

    @Test
    void searchByAccessionAndIncludeIsoForm() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL_ISOFORM);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_ISOFORM);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(
                                                SEARCH_RESOURCE
                                                        + "?query=accession:P21802&fields=accession,gene_primary&includeIsoform=true")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession",
                                containsInAnyOrder("P21802", "P21802-2")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession", not("P21802-1")));
    }

    @Test
    void searchIsoFormOnly() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL_ISOFORM);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_ISOFORM);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(
                                                SEARCH_RESOURCE
                                                        + "?query=((gene:FGFR2) AND (is_isoform:true))&fields=accession,gene_primary")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession", not("P21802")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession", not("P21802-1")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession", contains("P21802-2")));
    }

    @Test
    void canNotReturnFacetInformationForXML() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        String acc = entry.getPrimaryAccession().getValue();
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(
                                                SEARCH_RESOURCE
                                                        + "?query=accession:"
                                                        + acc
                                                        + "&facets=reviewed")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_XML_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE))
                .andExpect(
                        MockMvcResultMatchers.content()
                                .string(
                                        containsString(
                                                "Invalid content type received, 'application/xml'. Expected one of [application/json]")));
    }

    @Test
    @Tag("TRM_27084")
    void searchAllReturnsActiveOneEvenThoughMatchOnInactiveExists() throws Exception {
        // given
        UniProtKBEntry templateActiveEntry =
                UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        UniProtKBEntry activeDrome =
                UniProtKBEntryBuilder.from(templateActiveEntry).uniProtId("ACTIVE_DROME").build();
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, activeDrome);

        InactiveUniProtEntry inactiveDrome =
                InactiveUniProtEntry.from(
                        "I8FBX0",
                        "INACTIVE_DROME",
                        InactiveEntryMocker.DELETED,
                        "UPI0001661588",
                        null,
                        "SOURCE_DELETION_EMBL");
        getStoreManager()
                .saveEntriesInSolr(DataStoreManager.StoreType.INACTIVE_UNIPROT, inactiveDrome);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(SEARCH_RESOURCE + "?query=DROME")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.uniProtkbId",
                                Matchers.contains(activeDrome.getUniProtkbId().getValue())));
    }

    @ParameterizedTest
    @Tag("TRM_27083")
    @ValueSource(strings = {"reviewed", "unreviewed"})
    void canSearchForFullIDs(String type) throws Exception {
        // given
        String id = "GENE1_SPECIES";
        UniProtKBEntry templateEntry = UniProtEntryMocker.create(UniProtEntryMocker.Type.TR);
        if ("reviewed".equals(type)) {
            templateEntry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_COMPLEX);
        }

        UniProtKBEntry entry =
                UniProtKBEntryBuilder.from(templateEntry)
                        .uniProtId(id)
                        .primaryAccession("ACCESSION")
                        .build();

        getStoreManager()
                .save(
                        DataStoreManager.StoreType.UNIPROT,
                        UniProtEntryMocker.create(UniProtEntryMocker.Type.TR),
                        UniProtEntryMocker.create(UniProtEntryMocker.Type.SP),
                        entry,
                        UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_COMPLEX));

        // find exact entry for ID
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(
                                                String.format("%s?query=%s", SEARCH_RESOURCE, id))
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results[0].uniProtkbId", is(id)));
    }

    @ParameterizedTest
    @Tag("TRM_27083")
    @ValueSource(strings = {"XXXX1", "XXXX"})
    void canSearchForSwissProtEntryByFirstPartOfID(String idPart) throws Exception {
        // given
        String id = "XXXX1_SPECIES";
        UniProtKBEntry spEntry =
                UniProtKBEntryBuilder.from(
                                UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_COMPLEX))
                        .uniProtId(id)
                        .primaryAccession("ACCESSION")
                        .build();

        getStoreManager()
                .save(
                        DataStoreManager.StoreType.UNIPROT,
                        UniProtEntryMocker.create(UniProtEntryMocker.Type.TR),
                        spEntry,
                        UniProtEntryMocker.create(UniProtEntryMocker.Type.SP));

        // find Swiss-Prot entry by *part of first part of ID*, or all of first part of ID,
        // e.g. BRCA / BRCA2 in BRCA2_MOUSE
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(
                                                String.format(
                                                        "%s?query=%s", SEARCH_RESOURCE, idPart))
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results[0].uniProtkbId", is(id)));
    }

    @Test
    @Tag("TRM_27083")
    void canSearchForTrEMBLEntryByFirstPartOfID() throws Exception {
        // given
        String idPart = "XXXX1";
        String id = idPart + "_SPECIES";
        UniProtKBEntry trEntry =
                UniProtKBEntryBuilder.from(UniProtEntryMocker.create(UniProtEntryMocker.Type.TR))
                        .uniProtId(id)
                        .primaryAccession("ACCESSION")
                        .build();

        getStoreManager()
                .save(
                        DataStoreManager.StoreType.UNIPROT,
                        trEntry,
                        UniProtEntryMocker.create(UniProtEntryMocker.Type.SP));

        // find Swiss-Prot entry by *part of* first part of ID, e.g. BRCA in BRCA2_MOUSE
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(
                                                String.format(
                                                        "%s?query=%s", SEARCH_RESOURCE, idPart))
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results[0].uniProtkbId", is(id)));
    }

    @Test
    @Tag("TRM_27083")
    void cannotSearchForTrEMBLEntryByPartOfFirstPartOfID() throws Exception {
        // given
        String idPart = "XXXX";
        String id = idPart + "1_SPECIES";
        UniProtKBEntry trEntry =
                UniProtKBEntryBuilder.from(UniProtEntryMocker.create(UniProtEntryMocker.Type.TR))
                        .uniProtId(id)
                        .primaryAccession("ACCESSION")
                        .build();

        getStoreManager()
                .save(
                        DataStoreManager.StoreType.UNIPROT,
                        trEntry,
                        UniProtEntryMocker.create(UniProtEntryMocker.Type.SP));

        // find Swiss-Prot entry by *part of* first part of ID, e.g. BRCA in BRCA2_MOUSE
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(
                                                String.format(
                                                        "%s?query=%s", SEARCH_RESOURCE, idPart))
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(0)));
    }

    @ParameterizedTest
    @Tag("TRM_27083")
    @ValueSource(strings = {"reviewed", "unreviewed"})
    void canSearchForEntryBySecondPartOfID(String type) throws Exception {
        // given
        UniProtKBEntry templateEntry = UniProtEntryMocker.create(UniProtEntryMocker.Type.TR);
        if ("reviewed".equals(type)) {
            templateEntry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP);
        }

        String idPart = "SPECIES";
        String id = "GENE1_" + idPart;
        UniProtKBEntry entry =
                UniProtKBEntryBuilder.from(templateEntry)
                        .uniProtId(id)
                        .primaryAccession("ACCESSION")
                        .build();

        getStoreManager()
                .save(
                        DataStoreManager.StoreType.UNIPROT,
                        entry,
                        UniProtEntryMocker.create(UniProtEntryMocker.Type.SP));

        // find Swiss-Prot entry by *part of* first part of ID, e.g. BRCA in BRCA2_MOUSE
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(
                                                String.format(
                                                        "%s?query=%s", SEARCH_RESOURCE, idPart))
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results[0].uniProtkbId", is(id)));
    }

    @Test
    @Tag("TRM_26171")
    void searchAllReturnsActiveOne() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        List<InactiveUniProtEntry> deleted =
                InactiveEntryMocker.create(InactiveEntryMocker.InactiveType.DELETED);
        getStoreManager().saveEntriesInSolr(DataStoreManager.StoreType.INACTIVE_UNIPROT, deleted);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(SEARCH_RESOURCE + "?query=*")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession", containsInAnyOrder("P21802")));
    }

    @Test
    @Tag("TRM_26171")
    void searchForMergedInactiveEntriesByAccession() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        List<InactiveUniProtEntry> mergedList =
                InactiveEntryMocker.create(InactiveEntryMocker.InactiveType.MERGED);
        getStoreManager()
                .saveEntriesInSolr(DataStoreManager.StoreType.INACTIVE_UNIPROT, mergedList);

        // when accession field returns only itself
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(
                                                SEARCH_RESOURCE
                                                        + "?query=accession:q14301&fields=accession")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(1)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession", contains("Q14301")));

        // when default search returns only itself
        response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(
                                                SEARCH_RESOURCE + "?query=q14301&fields=accession")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(1)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession", contains("Q14301")));
    }

    @Test
    @Tag("TRM_26171")
    void searchForMergedInactiveEntriesById() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        List<InactiveUniProtEntry> mergedList =
                InactiveEntryMocker.create(InactiveEntryMocker.InactiveType.MERGED);
        getStoreManager()
                .saveEntriesInSolr(DataStoreManager.StoreType.INACTIVE_UNIPROT, mergedList);

        // when accession field returns only itself
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(
                                                SEARCH_RESOURCE + "?query=id:Q14301_FGFR2")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(1)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results[0].primaryAccession", is("Q14301")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results[0].uniProtkbId", is("Q14301_FGFR2")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results[0].extraAttributes.uniParcId", is("UPI000012CEBC")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results[0].inactiveReason.inactiveReasonType", is("MERGED")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results[0].inactiveReason.mergeDemergeTo", contains("P21802")));

        // when default search returns only itself
        response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(SEARCH_RESOURCE + "?query=Q14301_FGFR2")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(1)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession", contains("Q14301")));
    }

    @Test
    void searchForDeMergedInactiveEntriesByAccession() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        List<InactiveUniProtEntry> demergedList =
                InactiveEntryMocker.create(InactiveEntryMocker.InactiveType.DEMERGED);
        getStoreManager()
                .saveEntriesInSolr(DataStoreManager.StoreType.INACTIVE_UNIPROT, demergedList);

        // when search by accession field, returns only itself
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(
                                                SEARCH_RESOURCE + "?query=accession:Q00007")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(1)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession", contains("Q00007")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.entryType", contains("Inactive")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.extraAttributes.uniParcId", contains("UPI000012CEBF")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.inactiveReason.inactiveReasonType",
                                contains("DEMERGED")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.inactiveReason.mergeDemergeTo",
                                contains(contains("P21802", "P63151"))));

        // when search accession by default field, returns only itself
        response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(SEARCH_RESOURCE + "?query=Q00007")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(1)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession", contains("Q00007")));
    }

    @Test
    void searchForDeMergedInactiveEntriesById() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        List<InactiveUniProtEntry> demergedList =
                InactiveEntryMocker.create(InactiveEntryMocker.InactiveType.DEMERGED);
        getStoreManager()
                .saveEntriesInSolr(DataStoreManager.StoreType.INACTIVE_UNIPROT, demergedList);

        // when search by id field, return the active and inactive entries
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(
                                                SEARCH_RESOURCE + "?query=id:FGFR2_HUMAN")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(2)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession", contains("P21802", "Q00007")));

        // when search id by default field, it returns only the active ID
        response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(SEARCH_RESOURCE + "?query=FGFR2_HUMAN")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(1)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession", contains("P21802")));
    }

    @Test
    void searchForDeletedInactiveEntriesByAccession() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        List<InactiveUniProtEntry> deletedList =
                InactiveEntryMocker.create(InactiveEntryMocker.InactiveType.DELETED);
        getStoreManager()
                .saveEntriesInSolr(DataStoreManager.StoreType.INACTIVE_UNIPROT, deletedList);

        // when search accession by accession field, returns only itself
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(SEARCH_RESOURCE)
                                        .param("query", "accession:I8FBX1")
                                        .param("fields", "accession")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession", contains("I8FBX1")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.entryType", contains("Inactive")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.extraAttributes.uniParcId", contains("UPI00000DCD3D")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.inactiveReason.inactiveReasonType",
                                contains("DELETED")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.inactiveReason.deletedReason",
                                contains(DeletedReason.PROTEOME_REDUNDANCY.getName())));

        // when search accession by default field, returns only itself
        response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(SEARCH_RESOURCE + "?query=I8FBX1")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(1)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results[0].primaryAccession", is("I8FBX1")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.extraAttributes.uniParcId", contains("UPI00000DCD3D")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results[0].inactiveReason.inactiveReasonType", is("DELETED")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.inactiveReason.deletedReason",
                                contains(DeletedReason.PROTEOME_REDUNDANCY.getName())));
    }

    @Test
    void searchForDeletedInactiveEntriesById() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        List<InactiveUniProtEntry> deletedList =
                InactiveEntryMocker.create(InactiveEntryMocker.InactiveType.DELETED);
        getStoreManager()
                .saveEntriesInSolr(DataStoreManager.StoreType.INACTIVE_UNIPROT, deletedList);

        // when search accession by id field, returns only itself
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(
                                                SEARCH_RESOURCE + "?query=id:I8FBX2_YERPE")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession", contains("I8FBX2")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.entryType", contains("Inactive")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.extraAttributes.uniParcId", contains("UPI000012CEBB")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.inactiveReason.inactiveReasonType",
                                contains("DELETED")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.results.*.inactiveReason.deletedReason")
                                .doesNotExist());

        // when search accession by default field, returns only itself
        response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(SEARCH_RESOURCE + "?query=I8FBX2_YERPE")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession", contains("I8FBX2")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.extraAttributes.uniParcId", contains("UPI000012CEBB")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.inactiveReason.inactiveReasonType",
                                contains("DELETED")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.results.*.inactiveReason.deletedReason")
                                .doesNotExist());
    }

    @Test
    @Tag("TRM-28235")
    void searchForCanonicalIsoformForEntriesWithoutIsoformByAccession() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        // when search accession by id field, returns only itself
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(
                                                SEARCH_RESOURCE + "?query=accession:q8dia7-1")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession", contains("Q8DIA7")));

        // when search accession by default deefault search
        response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(SEARCH_RESOURCE + "?query=q8dia7-1")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession", contains("Q8DIA7")));
    }

    @Test
    void defaultSingleTermShowSingleTermMatchedFieldsReturnMatchedFields() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_ISOFORM);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL_ISOFORM);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(
                                                SEARCH_RESOURCE
                                                        + "?query=Familial&fields=accession,gene_primary&showSingleTermMatchedFields=true")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.matchedFields.size()", is(1)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.matchedFields.*.name", contains("cc_disease")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.matchedFields.*.hits", contains(1)));
    }

    @Test
    void notDefaultSearchWithMatchedFieldsDoesNotReturnMatchedFields() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(
                                                SEARCH_RESOURCE
                                                        + "?query=gene:FGFR2&fields=accession,gene_primary&showSingleTermMatchedFields=true")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession", contains("P21802")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.matchedFields").doesNotExist());
    }

    @Test
    void multiWordDefaultSearchWithMatchedFieldsDoesNotReturnMatchedFields() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(
                                                SEARCH_RESOURCE
                                                        + "?query=Fibroblast growth&fields=accession,gene_primary&showSingleTermMatchedFields=true")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession", contains("P21802")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.matchedFields").doesNotExist());
    }

    @Test
    void cannotReturnMatchedFieldsForXML() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(
                                                SEARCH_RESOURCE
                                                        + "?query=Familial&fields=accession,gene_primary&showSingleTermMatchedFields=true")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_XML_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE))
                .andExpect(
                        MockMvcResultMatchers.content()
                                .string(
                                        containsString(
                                                "Invalid content type received, 'application/xml'. Expected one of [application/json]")));
    }

    @Test
    void searchWhiteListPRODatabaseFieldDefaultSearch() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(SEARCH_RESOURCE)
                                        .param("query", "PR:P21802")
                                        .param("fields", "accession")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession", contains("P21802")));
    }

    @Test
    void searchWhiteListHGNCDatabaseFieldDefaultSearch() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(SEARCH_RESOURCE)
                                        .param("query", "HGNC:3689 AND accession:P21802")
                                        .param("fields", "accession,cc_interaction")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession", contains("P21802")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.comments[0].interactions[0].organismDiffer",
                                contains(true)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.comments[0].interactions[0].interactantOne.uniProtKBAccession",
                                contains("P21802")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.comments[0].interactions[0].interactantTwo.uniProtKBAccession",
                                contains("P03968")));
    }

    @Test
    void searchGoTermDefaultSearch() throws Exception {
        // we have a solr field named GO, so, it is not necessary add to white list
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(SEARCH_RESOURCE)
                                        .param("query", "GO:0016020")
                                        .param("fields", "accession")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession", contains("P21802")));
    }

    @Test
    void searchWithNotOperatorWorks() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.TR);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(SEARCH_RESOURCE)
                                        .param("query", "Reactome NOT(organism_id:9606)")
                                        .param("fields", "accession")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(1)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession", contains("F1Q0X3")));
    }

    @Test
    void searchWithNotWithDashOperatorWorks() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.TR);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(SEARCH_RESOURCE)
                                        .param("query", "Reactome -organism_id:9606")
                                        .param("fields", "accession")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(1)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession", contains("F1Q0X3")));
    }

    @Test
    void searchWithNotWithExclamationOperatorWorks() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.TR);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(SEARCH_RESOURCE)
                                        .param("query", "Reactome !organism_id:9606")
                                        .param("fields", "accession")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(1)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession", contains("F1Q0X3")));
    }

    @Test
    void searchWithCrossRefTCDBValue() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(SEARCH_RESOURCE)
                                        .param("query", "adiponectin")
                                        .param("fields", "accession")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(1)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession", contains("P21802")));
    }

    @Test
    void searchByIdShouldNotIncludeIsoform() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);
        UniProtKBId entryId = entry.getUniProtkbId();

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_ISOFORM);
        // set the same uniprot id as canonical
        UniProtKBEntryBuilder eb = UniProtKBEntryBuilder.from(entry);
        eb.uniProtId(entryId);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, eb.build());

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(
                                                SEARCH_RESOURCE
                                                        + "?query=id:FGFR2_HUMAN&fields=accession")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession", containsInAnyOrder("P21802")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession", not("P21802-2")));
    }

    @Test
    void searchByIdAndIncludeIsoform() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);
        UniProtKBId entryId = entry.getUniProtkbId();

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_ISOFORM);
        // set the same uniprot id as canonical
        UniProtKBEntryBuilder eb = UniProtKBEntryBuilder.from(entry);
        eb.uniProtId(entryId);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, eb.build());

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(
                                                SEARCH_RESOURCE
                                                        + "?query=id:FGFR2_HUMAN AND is_isoform:true&fields=accession")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession", containsInAnyOrder("P21802-2")));
    }

    @Test
    void searchDefaultSearchWithIsoform() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);
        UniProtKBId entryId = entry.getUniProtkbId();

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_ISOFORM);
        // set the same uniprot id as canonical
        UniProtKBEntryBuilder eb = UniProtKBEntryBuilder.from(entry);
        eb.uniProtId(entryId);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, eb.build());

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(SEARCH_RESOURCE + "?query=P21802-2")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession", containsInAnyOrder("P21802-2")));
    }

    @Test
    void searchDefaultSearchWithIsoformLowercase() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);
        UniProtKBId entryId = entry.getUniProtkbId();

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_ISOFORM);
        // set the same uniprot id as canonical
        UniProtKBEntryBuilder eb = UniProtKBEntryBuilder.from(entry);
        eb.uniProtId(entryId);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, eb.build());

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(SEARCH_RESOURCE + "?query=(p21802-2)")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession", containsInAnyOrder("P21802-2")));
    }

    @Test
    void searchDefaultSearchRheaUpperCase() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.TR);
        // Add rhea id that can be found as default search
        UniProtKBEntryBuilder eb = UniProtKBEntryBuilder.from(entry);
        ProteinDescriptionBuilder pd =
                ProteinDescriptionBuilder.from(entry.getProteinDescription());
        pd.allergenName(new NameBuilder().value("Injected RHEA:10596").build());
        eb.proteinDescription(pd.build());
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, eb.build());

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(SEARCH_RESOURCE)
                                        .param("query", "RHEA:10596")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(1)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results[0].primaryAccession", is("P21802")));
    }

    @Test
    void searchDefaultSearchWithUnderscoreIds() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.TR);
        // Add rhea id that can be found as default search
        UniProtKBEntryBuilder eb = UniProtKBEntryBuilder.from(entry);
        ProteinDescriptionBuilder pd =
                ProteinDescriptionBuilder.from(entry.getProteinDescription());
        pd.allergenName(new NameBuilder().value("Injected VAR test 004127").build());
        eb.proteinDescription(pd.build());
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, eb.build());

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(SEARCH_RESOURCE)
                                        .param("query", "VAR_004127")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(1)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results[0].primaryAccession", is("P21802")));
    }

    @Test
    void searchTestReturnDBSNP() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_ISOFORM);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL_ISOFORM);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(
                                                SEARCH_RESOURCE + "?query=*&fields=xref_dbsnp")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(1)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession", contains("P21802")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.*.features").exists())
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.results.*.uniProtKBCrossReferences")
                                .doesNotExist())
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results[0].features[0].type", is("Natural variant")));
    }

    @Test
    void searchGeneNameWithLeadingWildcardAndSingleMiddleWildcardSuccess() throws Exception {
        // given
        UniProtKBEntry canonicalEntry =
                UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, canonicalEntry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(SEARCH_RESOURCE + "?query=gene:*GFR*2")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.uniProtkbId",
                                Matchers.contains(canonicalEntry.getUniProtkbId().getValue())))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.genes[0].geneName.value", contains("FGFR2")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.warnings").doesNotExist());
    }

    @Test
    void defaultSearchWithLowercaseAccessionLetter() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(SEARCH_RESOURCE + "?query=(p21802)")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(1)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession", contains("P21802")));
    }

    //    TRM-28310
    @Test
    void searchDefaultSearchForAccessionAndGene() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.ACC);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.ACCANDGENE);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(SEARCH_RESOURCE + "?query=b3gat1")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));
        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(2)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results[0].primaryAccession", is("B3GAT1")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results[1].primaryAccession", is("Q9P2W7")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results[1].genes[0].geneName.value", is("B3GAT1")));
    }

    @Test
    void searchXrefFullForTSVFormatSuccess() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(SEARCH_RESOURCE)
                                        .param("query", "P21802")
                                        .param(
                                                "fields",
                                                "accession, xref_ensembl_full ,xref_interpro")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                UniProtMediaType.TSV_MEDIA_TYPE_VALUE));
        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(
                                        HttpHeaders.CONTENT_TYPE,
                                        UniProtMediaType.TSV_MEDIA_TYPE_VALUE))
                .andExpect(
                        MockMvcResultMatchers.content()
                                .string(
                                        containsString(
                                                "P21802\t\"ENST00000346997; ENSP00000263451; ENSG00000066468. [P21802-5]\";"
                                                        + "\"ENST00000356226; ENSP00000348559; ENSG00000066468. [P21802-20]\";"
                                                        + "\"ENST00000357555; ENSP00000350166; ENSG00000066468. [P21802-21]\";"
                                                        + "\"ENST00000358487; ENSP00000351276; ENSG00000066468. [P21802-1]\";"
                                                        + "\"ENST00000359354; ENSP00000352309; ENSG00000066468. [P21802-14]\";"
                                                        + "\"ENST00000360144; ENSP00000353262; ENSG00000066468. [P21802-22]\";"
                                                        + "\"ENST00000369056; ENSP00000358052; ENSG00000066468. [P21802-17]\";"
                                                        + "\"ENST00000369058; ENSP00000358054; ENSG00000066468. [P21802-13]\";"
                                                        + "\"ENST00000369060; ENSP00000358056; ENSG00000066468. [P21802-15]\";"
                                                        + "\"ENST00000369061; ENSP00000358057; ENSG00000066468. [P21802-23]\";"
                                                        + "\"ENST00000457416; ENSP00000410294; ENSG00000066468. [P21802-3]\";"
                                                        + "\tIPR016248;IPR041159;IPR007110;IPR036179;IPR013783;"
                                                        + "IPR013098;IPR003599;IPR003598;IPR011009;IPR000719;"
                                                        + "IPR017441;IPR001245;IPR008266;IPR020635;")));
    }

    @Test
    void searchXrefFullForSingleIdXrefAndJsonFormatReturnBadRequest() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(SEARCH_RESOURCE)
                                        .param("query", "P21802")
                                        .param(
                                                "fields",
                                                "accession, xref_ensembl_full ,xref_kegg_full")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));
        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.messages.size()", is(2)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.messages.*",
                                containsInAnyOrder(
                                        "Invalid fields parameter value 'xref_kegg_full'",
                                        "Fields 'xref_ensembl_full, xref_kegg_full' are only supported by TSV (text/plain;format=tsv) format.")));
    }

    @Test
    void searchWhitelistInvalidVGNCIdDefaultSearch() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.TR);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(SEARCH_RESOURCE)
                                        .param("query", "VGNC:sample38517")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.messages.*", contains("'VGNC' is not a valid search field")));
    }

    @Test
    void testSearchByChebi() throws Exception {
        saveEntry(SaveScenario.SEARCH_ALL_FIELDS);
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(SEARCH_RESOURCE)
                                        .param("query", "chebi:search")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(1)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results[0].primaryAccession", is("P00001")));
        // then

    }

    @Test
    void testSearchWithMoreThanAllowedOrClausesFailure() throws Exception {
        saveEntry(SaveScenario.SEARCH_ALL_FIELDS);
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(SEARCH_RESOURCE)
                                        .param(
                                                "query",
                                                "accession:P21802 OR accession:P00001 OR accession:P00002 OR accession:P00003 OR accession:P00004 OR accession:P00005 OR accession:P00006 OR accession:P00007 OR accession:P00008 OR accession:P00009 OR accession:P00010 OR accession:P00011 OR accession:P00012 OR accession:P00013 OR accession:P00014 OR accession:P00015 OR accession:P00016 OR accession:P00017 OR accession:P00018 OR accession:P00019 OR accession:P00020 OR accession:P00021 OR accession:P00022 OR accession:P00023 OR accession:P00024 OR accession:P00025 OR accession:P00026 OR accession:P00027 OR accession:P00028 OR accession:P00029 OR accession:P00030 OR accession:P00031 OR accession:P00032 OR accession:P00033 OR accession:P00034 OR accession:P00035 OR accession:P00036 OR accession:P00037 OR accession:P00038 OR accession:P00039 OR accession:P00040 OR accession:P00041 OR accession:P00042 OR accession:P00043 OR accession:P00044 OR accession:P00045 OR accession:P00046 OR accession:P00047 OR accession:P00048 OR accession:P00049 OR accession:P00050")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.url")
                                .value("http://localhost/uniprotkb/search"))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.messages[0]")
                                .value("Too many OR conditions in query. Maximum allowed is 50."));
    }

    @Test
    void searchWithChecksumValue() throws Exception {
        // given
        saveEntry(SaveScenario.SEARCH_SUCCESS);
        String md5 = "707AA6C6AD59761C0818C855C83A3188";

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getSearchRequestPath())
                                        .param("query", "checksum:" + md5)
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.results.*.sequence.md5", contains(md5)));
    }

    @Override
    protected String getSearchRequestPath() {
        return SEARCH_RESOURCE;
    }

    @Override
    protected int getDefaultPageSize() {
        return solrBatchSize;
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.UNIPROTKB;
    }

    @Override
    protected String getFieldValueForValidatedField(String searchField) {
        String value = "";
        if (searchField.startsWith("ftlen_") || searchField.startsWith("xref_count_")) {
            value = "[* TO *]";
        } else {
            value =
                    switch (searchField) {
                        case "accession", "accession_id" -> "P21802";
                        case "sec_acc" -> "B4DFC2";
                        case "organism_id", "virus_host_id", "taxonomy_id" -> "9606";
                        case "length", "mass" -> "[* TO *]";
                        case "proteome" -> "UP000000000";
                        case "annotation_score" -> "5";
                        case "uniref_cluster_50" -> "UniRef50_P00001";
                        case "uniref_cluster_90" -> "UniRef90_P00001";
                        case "uniref_cluster_100" -> "UniRef100_P00001";
                        case "uniparc" -> "UPI0000000001";
                        case "existence" -> "1";
                        case "date_modified",
                                "date_created",
                                "date_sequence_modified",
                                "lit_pubdate" -> {
                            String now = Instant.now().toString();
                            yield "[* TO " + now + "]";
                        }
                        default -> value;
                    };
        }
        return value;
    }

    @Override
    protected List<String> getAllFacetFields() {
        return new ArrayList<>(facetConfig.getFacetNames());
    }

    @Override
    protected String getAllReturnedFieldsQuery() {
        return "id:*";
    }

    @Override
    protected void saveEntry(SaveScenario saveContext) {
        UniProtEntryConverter converter = new UniProtEntryConverter(new HashMap<>(), new HashMap<>());
        UniProtKBEntry entry =
                UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL); // P21802
        UniProtDocument document = converter.convert(entry);
        UniProtKBEntryConvertITUtils.aggregateTaxonomyDataToDocument(taxonomyRepo, document);
        getStoreManager().saveDocs(DataStoreManager.StoreType.UNIPROT, document);
        getStoreManager().saveToStore(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP); // Q8DIA7
        document = converter.convert(entry);
        UniProtKBEntryConvertITUtils.aggregateTaxonomyDataToDocument(taxonomyRepo, document);
        getStoreManager().saveDocs(DataStoreManager.StoreType.UNIPROT, document);
        getStoreManager().saveToStore(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_ISOFORM); // P21802-2
        document = converter.convert(entry);
        UniProtKBEntryConvertITUtils.aggregateTaxonomyDataToDocument(taxonomyRepo, document);
        getStoreManager().saveDocs(DataStoreManager.StoreType.UNIPROT, document);
        getStoreManager().saveToStore(DataStoreManager.StoreType.UNIPROT, entry);

        if (SaveScenario.SEARCH_ALL_FIELDS.equals(saveContext)
                || SaveScenario.SEARCH_ALL_RETURN_FIELDS.equals(saveContext)
                || SaveScenario.FACETS_SUCCESS.equals(saveContext)) {

            InactiveUniProtEntry inactiveDrome =
                    InactiveUniProtEntry.from(
                            "I8FBX0",
                            "INACTIVE_DROME",
                            InactiveEntryMocker.DELETED,
                            "UPI0001661588",
                            null,
                            "SOURCE_DELETION_EMBL");
            getStoreManager()
                    .saveEntriesInSolr(DataStoreManager.StoreType.INACTIVE_UNIPROT, inactiveDrome);

            UniProtDocument doc = new UniProtDocument();
            doc.accession = "P00001";
            doc.id.add("Search All");
            doc.active = true;
            doc.fragment = true;
            doc.precursor = true;
            doc.isIsoform = false;
            doc.evidenceExperimental = true;
            doc.otherOrganism = "Search All";
            doc.organismHostIds.add(9606);
            doc.organismHostNames.add("Search All");
            doc.encodedIn.add("Search All");
            doc.rcPlasmid.add("Search All");
            doc.rcTransposon.add("Search All");
            doc.rcStrain.add("Search All");
            doc.rcTissue.add("Search All");
            doc.subcellLocationNote.add("Search All");
            doc.commentMap.put("cc_scl_note_exp", Collections.singleton("Search All"));
            doc.subcellLocationTerm.add("Search All");
            doc.commentMap.put("cc_scl_term_exp", Collections.singleton("Search All"));
            doc.ap.add("Search All");
            doc.commentMap.put("cc_ap_exp", Collections.singleton("Search All"));
            doc.apAi.add("Search All");
            doc.commentMap.put("cc_ap_ai_exp", Collections.singleton("Search All"));
            doc.apApu.add("Search All");
            doc.commentMap.put("cc_ap_apu_exp", Collections.singleton("Search All"));
            doc.apAs.add("Search All");
            doc.commentMap.put("cc_ap_as_exp", Collections.singleton("Search All"));
            doc.apRf.add("Search All");
            doc.commentMap.put("cc_ap_rf_exp", Collections.singleton("Search All"));
            doc.bpcp.add("Search All");
            doc.commentMap.put("cc_bpcp_exp", Collections.singleton("Search All"));
            doc.bpcpAbsorption.add("Search All");
            doc.commentMap.put("cc_bpcp_absorption_exp", Collections.singleton("Search All"));
            doc.bpcpKinetics.add("Search All");
            doc.commentMap.put("cc_bpcp_kinetics_exp", Collections.singleton("Search All"));
            doc.bpcpPhDependence.add("Search All");
            doc.commentMap.put("cc_bpcp_ph_dependence_exp", Collections.singleton("Search All"));
            doc.bpcpRedoxPotential.add("Search All");
            doc.commentMap.put("cc_bpcp_redox_potential_exp", Collections.singleton("Search All"));
            doc.bpcpTempDependence.add("Search All");
            doc.commentMap.put("cc_bpcp_temp_dependence_exp", Collections.singleton("Search All"));
            doc.inchikey.add("Search All");
            doc.rheaIds.add("Search All");
            doc.chebi.addAll(Set.of("Search All", "CHEBI:12345"));
            doc.cofactorChebi.add("Search All");
            doc.commentMap.put("cc_cofactor_chebi_exp", Collections.singleton("Search All"));
            doc.cofactorNote.add("Search All");
            doc.commentMap.put("cc_cofactor_note_exp", Collections.singleton("Search All"));
            doc.seqCaution.add("Search All");
            doc.commentMap.put("cc_sc_exp", Collections.singleton("Search All"));
            doc.seqCautionFrameshift.add("Search All");
            doc.seqCautionErInit.add("Search All");
            doc.seqCautionErPred.add("Search All");
            doc.seqCautionErTerm.add("Search All");
            doc.seqCautionErTran.add("Search All");
            doc.seqCautionMisc.add("Search All");
            doc.commentMap.put("cc_sc_misc_exp", Collections.singleton("Search All"));
            doc.proteomes.add("UP000000000");
            doc.uniparc = "UPI0000000001";
            doc.unirefCluster50 = "UniRef50_P00001";
            doc.unirefCluster90 = "UniRef90_P00001";
            doc.unirefCluster100 = "UniRef100_P00001";
            doc.computationalPubmedIds.add("890123456");
            doc.communityPubmedIds.add("1234567");
            UniProtDatabaseTypes.INSTANCE
                    .getUniProtKBDbTypes()
                    .forEach(
                            db -> {
                                doc.xrefCountMap.put(
                                        "xref_count_" + db.getName().toLowerCase(), 0L);
                            });

            Arrays.stream(UniprotKBFeatureType.values())
                    .forEach(
                            type -> {
                                String typeName = type.getName().toLowerCase();
                                doc.featuresMap.put(
                                        "ft_" + typeName, Collections.singleton("Search All"));
                                doc.featuresMap.put(
                                        "ft_" + typeName + "_exp",
                                        Collections.singleton("Search All"));
                            });

            Arrays.stream(FeatureCategory.values())
                    .map(type -> type.getName().toLowerCase())
                    .forEach(
                            type -> {
                                doc.featuresMap.put(
                                        "ft_" + type, Collections.singleton("Search All"));
                                doc.featuresMap.put(
                                        "ft_" + type + "_exp", Collections.singleton("Search All"));
                            });

            Arrays.stream(CommentType.values())
                    .forEach(
                            type -> {
                                String typeName = type.name().toLowerCase();
                                doc.commentMap.put(
                                        "cc_" + typeName, Collections.singleton("Search All"));
                                doc.commentMap.put(
                                        "cc_" + typeName + "_exp",
                                        Collections.singleton("Search All"));
                            });

            List<String> goAssertionCodes =
                    GoEvidences.INSTANCE.getEvidences().stream()
                            .filter(this::getManualEvidenceGroup)
                            .flatMap(this::getEvidenceCodes)
                            .map(String::toLowerCase)
                            .collect(Collectors.toList());

            goAssertionCodes.addAll(Arrays.asList("rca", "nd", "ibd", "ikr", "ird", "unknown"));

            goAssertionCodes.forEach(
                    code ->
                            doc.goWithEvidenceMaps.put(
                                    "go_" + code, Collections.singleton("Search All")));

            entry = UniProtKBEntryIT.getCompleteColumnsUniProtEntry();

            TaxonomyEntry taxonomyEntry = TaxonomyEntryTest.getCompleteTaxonomyEntry();
            TaxonomyDocument taxDoc =
                    TaxonomyDocument.builder()
                            .id("9606")
                            .taxId(9606L)
                            .taxonomyObj(getTaxonomyBinary(taxonomyEntry))
                            .build();
            getStoreManager().saveDocs(DataStoreManager.StoreType.TAXONOMY, taxDoc);
            taxDoc =
                    TaxonomyDocument.builder()
                            .id("197221")
                            .taxId(197221L)
                            .taxonomyObj(getTaxonomyBinary(taxonomyEntry))
                            .build();
            getStoreManager().saveDocs(DataStoreManager.StoreType.TAXONOMY, taxDoc);

            getStoreManager().saveDocs(DataStoreManager.StoreType.UNIPROT, doc);
            getStoreManager().saveToStore(DataStoreManager.StoreType.UNIPROT, entry);
        }
    }

    private byte[] getTaxonomyBinary(TaxonomyEntry entry) {
        try {
            return TaxonomyJsonConfig.getInstance().getFullObjectMapper().writeValueAsBytes(entry);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to parse TaxonomyEntry to binary json: ", e);
        }
    }

    private boolean getManualEvidenceGroup(EvidenceGroup evidenceGroup) {
        return evidenceGroup.getGroupName().equalsIgnoreCase("Manual assertions");
    }

    private Stream<String> getEvidenceCodes(EvidenceGroup evidenceGroup) {
        return evidenceGroup.getItems().stream().map(EvidenceItem::getCode);
    }

    @Override
    protected void saveEntries(int numberOfEntries) {
        LongStream.rangeClosed(1, numberOfEntries)
                .forEach(
                        i -> {
                            UniProtDocument doc = new UniProtDocument();
                            doc.accession = "P0000" + i;
                            doc.active = true;
                            doc.isIsoform = false;

                            UniProtKBEntry entry =
                                    new UniProtKBEntryBuilder(
                                                    "P0000" + i,
                                                    "P12345_ID",
                                                    UniProtKBEntryType.TREMBL)
                                            .build();

                            getStoreManager().saveDocs(DataStoreManager.StoreType.UNIPROT, doc);
                            getStoreManager()
                                    .saveToStore(DataStoreManager.StoreType.UNIPROT, entry);
                        });
    }

    @Override
    protected List<Triple<String, String, List<String>>> getTriplets() {
        return List.of(
                Triple.of("protein_name", "fibroblost", List.of("fibroblast")),
                Triple.of("taxonomy_name", "\"homo sapeans\"", List.of("\"homo sapiens\"")),
                Triple.of("cc_disease", "pfeifer", List.of("pfeiffer")),
                Triple.of("accession_id", "p218o2", List.of("p21802")),
                Triple.of("id", "p218o2", List.of("p21802")),
                Triple.of("gene_exact", "fgfr9", List.of("fgfr2", "fgfr", "fgar")));
    }

    static class UniprotKBSearchParameterResolver extends AbstractSearchParameterResolver {

        @Override
        protected SearchParameter searchCanReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("accession:p21802"))
                    .resultMatcher(
                            MockMvcResultMatchers.jsonPath(
                                    "$.results.*.primaryAccession", contains("P21802")))
                    .build();
        }

        @Override
        protected SearchParameter searchCanReturnNotFoundParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("accession:P12345"))
                    .resultMatcher(MockMvcResultMatchers.jsonPath("$.results.size()", is(0)))
                    .build();
        }

        @Override
        protected SearchParameter searchAllowWildcardQueryAllDocumentsParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("organism_name:*"))
                    .resultMatcher(
                            MockMvcResultMatchers.jsonPath(
                                    "$.results.*.primaryAccession",
                                    contains(ACCESSION_SP_CANONICAL, ACCESSION_SP)))
                    .resultMatcher(
                            MockMvcResultMatchers.jsonPath(
                                    "$.results.*.organism.taxonId",
                                    containsInAnyOrder(9606, 197221)))
                    .build();
        }

        @Override
        protected SearchParameter searchQueryWithInvalidTypeQueryReturnBadRequestParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("gene:[1 TO 10]"))
                    .resultMatcher(
                            MockMvcResultMatchers.jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(
                            MockMvcResultMatchers.jsonPath(
                                    "$.messages.*",
                                    contains(
                                            "'gene' filter type 'range' is invalid. Expected 'general' filter type")))
                    .build();
        }

        @Override
        protected SearchParameter searchQueryWithInvalidValueQueryReturnBadRequestParameter() {
            return SearchParameter.builder()
                    .queryParam(
                            "query",
                            Collections.singletonList(
                                    "accession:INVALID OR accession_id:INVALID "
                                            + "OR reviewed:INVALID OR organism_id:invalid OR virus_host_id:invalid OR taxonomy_id:invalid "
                                            + "OR is_isoform:invalid OR structure_3d:invalid OR active:invalid OR proteome:INVALID"
                                            + "OR uniparc:invalid OR uniref_cluster_50:invalid OR uniref_cluster_90:invalid "
                                            + "OR uniref_cluster_100:invalid OR fragment:INVALID OR precursor:INVALID"
                                            + "OR existence:6"))
                    .resultMatcher(
                            MockMvcResultMatchers.jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(
                            MockMvcResultMatchers.jsonPath(
                                    "$.messages.*",
                                    containsInAnyOrder(
                                            "The 'accession' filter value 'INVALID' has invalid format. It should be a valid UniProtKB accession",
                                            "The 'proteome' filter value has invalid format. It should match the regular expression UP[0-9]{9}",
                                            "The 'is_isoform' filter value can only be true or false",
                                            "The 'reviewed' filter value can only be true or false",
                                            "The 'fragment' filter value can only be true or false",
                                            "The 'precursor' filter value can only be true or false",
                                            "The 'active' parameter can only be true or false",
                                            "The 'organism_id' filter value should be a number",
                                            "The 'structure_3d' filter value can only be true or false",
                                            "The 'taxonomy_id' filter value should be a number",
                                            "The 'accession_id' filter value 'INVALID' has invalid format. It should be a valid UniProtKB accession",
                                            "The 'virus_host_id' filter value should be a number",
                                            "The 'uniparc' filter value has invalid format. It should be a valid UniParc UPI.",
                                            "The 'uniref_cluster_50' filter value has invalid format. It should be a valid UniRef50 cluster ID.",
                                            "The 'uniref_cluster_90' filter value has invalid format. It should be a valid UniRef90 cluster ID.",
                                            "The 'uniref_cluster_100' filter value has invalid format. It should be a valid UniRef100 cluster ID.",
                                            "The 'existence' filter value should be a number from 1 to 5")))
                    .build();
        }

        @Override
        protected SearchParameter searchSortWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("sort", Collections.singletonList("gene desc"))
                    .resultMatcher(
                            MockMvcResultMatchers.jsonPath(
                                    "$.results.*.primaryAccession",
                                    contains(ACCESSION_SP, ACCESSION_SP_CANONICAL)))
                    .resultMatcher(
                            MockMvcResultMatchers.jsonPath(
                                    "$.results.*.genes[0].geneName.value",
                                    contains("purL", "FGFR2")))
                    .build();
        }

        @Override
        protected SearchParameter searchFieldsWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam(
                            "fields",
                            Collections.singletonList("accession,gene_primary,protein_name"))
                    .resultMatcher(
                            MockMvcResultMatchers.jsonPath(
                                    "$.results.*.primaryAccession",
                                    contains(ACCESSION_SP_CANONICAL, ACCESSION_SP)))
                    .resultMatcher(
                            MockMvcResultMatchers.jsonPath("$.results.*.proteinDescription")
                                    .exists())
                    .resultMatcher(MockMvcResultMatchers.jsonPath("$.results.*.genes").exists())
                    .resultMatcher(
                            MockMvcResultMatchers.jsonPath("$.results.*.comments").doesNotExist())
                    .resultMatcher(
                            MockMvcResultMatchers.jsonPath("$.results.*.features").doesNotExist())
                    .resultMatcher(
                            MockMvcResultMatchers.jsonPath("$.results.*.keywords").doesNotExist())
                    .resultMatcher(
                            MockMvcResultMatchers.jsonPath("$.results.*.references").doesNotExist())
                    .resultMatcher(
                            MockMvcResultMatchers.jsonPath("$.results.*.sequence").doesNotExist())
                    .build();
        }

        @Override
        protected SearchParameter searchFacetsWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("facets", Collections.singletonList("reviewed,fragment"))
                    .resultMatcher(
                            MockMvcResultMatchers.jsonPath(
                                    "$.results.*.primaryAccession",
                                    contains(ACCESSION_SP_CANONICAL, ACCESSION_SP, "P00001")))
                    .resultMatcher(MockMvcResultMatchers.jsonPath("$.facets", notNullValue()))
                    .resultMatcher(MockMvcResultMatchers.jsonPath("$.facets", not(empty())))
                    .resultMatcher(
                            MockMvcResultMatchers.jsonPath(
                                    "$.facets.*.name", containsInAnyOrder("reviewed", "fragment")))
                    .build();
        }
    }

    static class UniprotKBSearchContentTypeParamResolver
            extends AbstractSearchContentTypeParamResolver {

        @Override
        protected SearchContentTypeParam searchSuccessContentTypesParam() {
            return SearchContentTypeParam.builder()
                    .query("accession:" + ACCESSION_SP_CANONICAL + " OR accession:" + ACCESSION_SP)
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(
                                            MockMvcResultMatchers.jsonPath(
                                                    "$.results.*.primaryAccession",
                                                    contains(ACCESSION_SP_CANONICAL, ACCESSION_SP)))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_XML)
                                    .resultMatcher(
                                            MockMvcResultMatchers.content()
                                                    .string(
                                                            startsWith(
                                                                    ConverterConstants
                                                                                    .XML_DECLARATION
                                                                            + ConverterConstants
                                                                                    .UNIPROTKB_XML_SCHEMA)))
                                    .resultMatcher(
                                            MockMvcResultMatchers.content()
                                                    .string(
                                                            containsString(
                                                                    "<accession>P21802</accession>")))
                                    .resultMatcher(
                                            MockMvcResultMatchers.content()
                                                    .string(
                                                            containsString(
                                                                    "<accession>Q8DIA7</accession>")))
                                    .resultMatcher(
                                            MockMvcResultMatchers.content()
                                                    .string(
                                                            endsWith(
                                                                    ConverterConstants.COPYRIGHT_TAG
                                                                            + ConverterConstants
                                                                                    .UNIPROTKB_XML_CLOSE_TAG)))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.FF_MEDIA_TYPE)
                                    .resultMatcher(
                                            MockMvcResultMatchers.content()
                                                    .string(containsString("AC   P21802;")))
                                    .resultMatcher(
                                            MockMvcResultMatchers.content()
                                                    .string(containsString("AC   Q8DIA7;")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.FASTA_MEDIA_TYPE)
                                    .resultMatcher(
                                            MockMvcResultMatchers.content()
                                                    .string(
                                                            containsString(
                                                                    ">sp|Q8DIA7|"
                                                                            + "PURL_THEEB Phosphoribosylformylglycinamidine synthase subunit PurL "
                                                                            + "OS=Thermosynechococcus elongatus (strain BP-1) OX=197221 GN=purL PE=3 SV=1")))
                                    .resultMatcher(
                                            MockMvcResultMatchers.content()
                                                    .string(
                                                            containsString(
                                                                    ">sp|P21802|FGFR2_HUMAN Fibroblast"
                                                                            + " growth factor receptor 2 OS=Homo sapiens OX=9606 GN=FGFR2 PE=1 SV=1")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.GFF_MEDIA_TYPE)
                                    .resultMatcher(
                                            MockMvcResultMatchers.content()
                                                    .string(containsString("##gff-version 3")))
                                    .resultMatcher(
                                            MockMvcResultMatchers.content()
                                                    .string(
                                                            containsString(
                                                                    "P21802\tUniProtKB\tSignal peptide\t1\t21\t.\t.\t.\tOntology_term=ECO:0000255;evidence=ECO:0000255")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(
                                            MockMvcResultMatchers.content()
                                                    .string(containsString(ACCESSION_SP_CANONICAL)))
                                    .resultMatcher(
                                            MockMvcResultMatchers.content()
                                                    .string(containsString(ACCESSION_SP)))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(
                                            MockMvcResultMatchers.content()
                                                    .string(
                                                            containsString(
                                                                    "Entry\tEntry Name\tReviewed\tProtein names\tGene Names\tOrganism\tLength")))
                                    .resultMatcher(
                                            MockMvcResultMatchers.content()
                                                    .string(
                                                            containsString(
                                                                    "Q8DIA7\tPURL_THEEB\treviewed\tPhosphoribosylformylglycinamidine synthase subunit PurL (FGAM synthase)")))
                                    .resultMatcher(
                                            MockMvcResultMatchers.content()
                                                    .string(
                                                            containsString(
                                                                    "P21802\tFGFR2_HUMAN\treviewed\tFibroblast growth factor receptor 2 (FGFR-2)")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                                    .resultMatcher(
                                            MockMvcResultMatchers.content()
                                                    .contentType(UniProtMediaType.XLS_MEDIA_TYPE))
                                    .build())
                    .build();
        }

        @Override
        protected SearchContentTypeParam searchBadRequestContentTypesParam() {
            return SearchContentTypeParam.builder()
                    .query("accession:invalid")
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(
                                            MockMvcResultMatchers.jsonPath(
                                                    "$.url", not(isEmptyOrNullString())))
                                    .resultMatcher(
                                            MockMvcResultMatchers.jsonPath(
                                                    "$.messages.*",
                                                    contains(
                                                            "The 'accession' filter value 'invalid' has invalid format. It should be a valid UniProtKB accession")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_XML)
                                    .resultMatcher(
                                            MockMvcResultMatchers.content()
                                                    .string(
                                                            containsString(
                                                                    "<messages>The 'accession' filter value 'invalid' has invalid format. It should be a valid UniProtKB accession</messages>")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.FASTA_MEDIA_TYPE)
                                    .resultMatcher(
                                            MockMvcResultMatchers.content()
                                                    .string(
                                                            is(
                                                                    "Error messages\nThe 'accession' filter value 'invalid' has invalid format. It should be a valid UniProtKB accession")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.GFF_MEDIA_TYPE)
                                    .resultMatcher(
                                            MockMvcResultMatchers.content()
                                                    .string(
                                                            is(
                                                                    "Error messages\nThe 'accession' filter value 'invalid' has invalid format. It should be a valid UniProtKB accession")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.FF_MEDIA_TYPE)
                                    .resultMatcher(
                                            MockMvcResultMatchers.content()
                                                    .string(
                                                            is(
                                                                    "Error messages\nThe 'accession' filter value 'invalid' has invalid format. It should be a valid UniProtKB accession")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(
                                            MockMvcResultMatchers.content()
                                                    .string(
                                                            is(
                                                                    "Error messages\nThe 'accession' filter value 'invalid' has invalid format. It should be a valid UniProtKB accession")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(
                                            MockMvcResultMatchers.content()
                                                    .string(
                                                            is(
                                                                    "Error messages\nThe 'accession' filter value 'invalid' has invalid format. It should be a valid UniProtKB accession")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                                    .resultMatcher(
                                            MockMvcResultMatchers.content()
                                                    .contentType(UniProtMediaType.XLS_MEDIA_TYPE))
                                    .build())
                    .build();
        }
    }
}
