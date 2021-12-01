package org.uniprot.api.uniprotkb.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
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
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.rest.controller.AbstractSearchWithFacetControllerIT;
import org.uniprot.api.rest.controller.SaveScenario;
import org.uniprot.api.rest.controller.param.ContentTypeParam;
import org.uniprot.api.rest.controller.param.SearchContentTypeParam;
import org.uniprot.api.rest.controller.param.SearchParameter;
import org.uniprot.api.rest.controller.param.resolver.AbstractSearchContentTypeParamResolver;
import org.uniprot.api.rest.controller.param.resolver.AbstractSearchParameterResolver;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.respository.facet.impl.UniProtKBFacetConfig;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.api.uniprotkb.UniProtKBREST;
import org.uniprot.api.uniprotkb.repository.DataStoreTestConfig;
import org.uniprot.api.uniprotkb.repository.search.impl.TaxonomyRepository;
import org.uniprot.api.uniprotkb.repository.search.impl.UniprotQueryRepository;
import org.uniprot.api.uniprotkb.repository.store.UniProtKBStoreClient;
import org.uniprot.core.json.parser.taxonomy.TaxonomyEntryTest;
import org.uniprot.core.json.parser.taxonomy.TaxonomyJsonConfig;
import org.uniprot.core.json.parser.uniprot.UniProtKBEntryIT;
import org.uniprot.core.taxonomy.TaxonomyEntry;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.uniprotkb.UniProtKBEntryType;
import org.uniprot.core.uniprotkb.comment.CommentType;
import org.uniprot.core.uniprotkb.description.impl.NameBuilder;
import org.uniprot.core.uniprotkb.description.impl.ProteinDescriptionBuilder;
import org.uniprot.core.uniprotkb.description.impl.ProteinNameBuilder;
import org.uniprot.core.uniprotkb.feature.FeatureCategory;
import org.uniprot.core.uniprotkb.feature.UniprotKBFeatureType;
import org.uniprot.core.uniprotkb.impl.UniProtKBEntryBuilder;
import org.uniprot.cv.chebi.ChebiRepo;
import org.uniprot.cv.ec.ECRepo;
import org.uniprot.cv.go.GORepo;
import org.uniprot.cv.xdb.UniProtDatabaseTypes;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.datastore.voldemort.uniprot.VoldemortInMemoryUniprotEntryStore;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.indexer.uniprot.inactiveentry.InactiveUniProtEntry;
import org.uniprot.store.indexer.uniprot.mockers.InactiveEntryMocker;
import org.uniprot.store.indexer.uniprot.mockers.PathwayRepoMocker;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.indexer.uniprot.mockers.UniProtEntryMocker;
import org.uniprot.store.indexer.uniprotkb.converter.UniProtEntryConverter;
import org.uniprot.store.indexer.uniprotkb.processor.InactiveEntryConverter;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.taxonomy.TaxonomyDocument;
import org.uniprot.store.search.document.uniprot.UniProtDocument;
import org.uniprot.store.search.domain.EvidenceGroup;
import org.uniprot.store.search.domain.EvidenceItem;
import org.uniprot.store.search.domain.impl.GoEvidences;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.uniprot.api.rest.output.converter.ConverterConstants.*;
import static org.uniprot.api.uniprotkb.controller.UniProtKBController.UNIPROTKB_RESOURCE;

@ContextConfiguration(
        classes = {DataStoreTestConfig.class, UniProtKBREST.class, ErrorHandlerConfig.class})
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
class UniProtKBSearchControllerIT extends AbstractSearchWithFacetControllerIT {

    private static final String SEARCH_RESOURCE = UNIPROTKB_RESOURCE + "/search";

    private static final String ACCESSION_SP_CANONICAL = "P21802";
    private static final String ACCESSION_SP = "Q8DIA7";

    @Autowired private UniprotQueryRepository repository;

    @Autowired private TaxonomyRepository taxRepository;

    @Value("${search.default.page.size:#{null}}")
    private Integer solrBatchSize;

    private UniProtKBStoreClient storeClient;

    @Autowired private UniProtKBFacetConfig facetConfig;

    @BeforeAll
    void initUniprotKbDataStore() {
        DataStoreManager dsm = getStoreManager();
        dsm.addDocConverter(
                DataStoreManager.StoreType.UNIPROT,
                new UniProtEntryConverter(
                        TaxonomyRepoMocker.getTaxonomyRepo(),
                        Mockito.mock(GORepo.class),
                        PathwayRepoMocker.getPathwayRepo(),
                        Mockito.mock(ChebiRepo.class),
                        Mockito.mock(ECRepo.class),
                        new HashMap<>()));
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
    void searchInvalidIncludeIsoformParameterValue() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        String acc = entry.getPrimaryAccession().getValue();
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(SEARCH_RESOURCE
                                                + "?query=accession:P21802&includeIsoform=invalid")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains(
                                        "Invalid includeIsoform parameter value. Expected true or false")));
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
                                get(SEARCH_RESOURCE
                                                + "?query=(Serine/arginine repetitive matrix protein 2)")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.results.*.primaryAccession", contains(acc)));
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
                                get(SEARCH_RESOURCE
                                                + "?query=(\"Serine/arginine repetitive matrix protein 2\")")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.results.*.primaryAccession", contains(acc)));
    }

    @Test
    void searchSecondaryAccession() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        String acc = entry.getPrimaryAccession().getValue();
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_ISOFORM);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL_ISOFORM);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(SEARCH_RESOURCE
                                                + "?query=sec_acc:B4DFC2&fields=accession,gene_primary")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.results.*.primaryAccession", contains("P21802")));
    }

    @Test
    void searchCanonicalOnly() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        String acc = entry.getPrimaryAccession().getValue();
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL_ISOFORM);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_ISOFORM);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(SEARCH_RESOURCE
                                                + "?query=accession:P21802&fields=accession,gene_primary")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.results.*.primaryAccession", contains("P21802")))
                .andExpect(jsonPath("$.results.*.primaryAccession", not("P21802-1")))
                .andExpect(jsonPath("$.results.*.primaryAccession", not("P21802-2")));
    }

    @Test
    void searchCanonicalIsoformAccession() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        String acc = entry.getPrimaryAccession().getValue();
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL_ISOFORM);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_ISOFORM);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(SEARCH_RESOURCE
                                                + "?query=accession_id:P21802-1&fields=accession,gene_primary")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.*.primaryAccession", not("P21802")))
                .andExpect(jsonPath("$.results.*.primaryAccession", contains("P21802-1")))
                .andExpect(jsonPath("$.results.*.primaryAccession", not("P21802-2")));
    }

    @Test
    void searchIncludeCanonicalAndIsoForm() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        String acc = entry.getPrimaryAccession().getValue();
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL_ISOFORM);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_ISOFORM);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(SEARCH_RESOURCE
                                                + "?query=gene:FGFR2&fields=accession,gene_primary&includeIsoform=true")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.results.*.primaryAccession",
                                containsInAnyOrder("P21802", "P21802-2")))
                .andExpect(jsonPath("$.results.*.primaryAccession", not("P21802-1")));
    }

    @Test
    void searchByAccessionAndIncludeIsoForm() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        String acc = entry.getPrimaryAccession().getValue();
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL_ISOFORM);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_ISOFORM);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(SEARCH_RESOURCE
                                                + "?query=accession:P21802&fields=accession,gene_primary&includeIsoform=true")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.results.*.primaryAccession",
                                containsInAnyOrder("P21802", "P21802-2")))
                .andExpect(jsonPath("$.results.*.primaryAccession", not("P21802-1")));
    }

    @Test
    void searchIsoFormOnly() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        String acc = entry.getPrimaryAccession().getValue();
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL_ISOFORM);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_ISOFORM);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(SEARCH_RESOURCE
                                                + "?query=((gene:FGFR2) AND (is_isoform:true))&fields=accession,gene_primary")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.*.primaryAccession", not("P21802")))
                .andExpect(jsonPath("$.results.*.primaryAccession", not("P21802-1")))
                .andExpect(jsonPath("$.results.*.primaryAccession", contains("P21802-2")));
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
                                get(SEARCH_RESOURCE
                                                + "?query=accession:"
                                                + acc
                                                + "&facets=reviewed")
                                        .header(ACCEPT, APPLICATION_XML_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_XML_VALUE))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "Invalid content type received, 'application/xml'. Expected one of [application/json]")));
    }

    @Test
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
                                get(SEARCH_RESOURCE + "?query=*")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.*.primaryAccession", containsInAnyOrder("P21802")));
    }

    @Test
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
                                get(SEARCH_RESOURCE + "?query=accession:Q14301&fields=accession")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.results.*.primaryAccession", contains("Q14301")));

        // when default search returns only itself
        response =
                getMockMvc()
                        .perform(
                                get(SEARCH_RESOURCE + "?query=Q14301&fields=accession")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.results.*.primaryAccession", contains("Q14301")));
    }

    @Test
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
                                get(SEARCH_RESOURCE + "?query=id:Q14301_FGFR2")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.results[0].primaryAccession", is("Q14301")))
                .andExpect(jsonPath("$.results[0].uniProtkbId", is("Q14301_FGFR2")))
                .andExpect(jsonPath("$.results[0].inactiveReason.inactiveReasonType", is("MERGED")))
                .andExpect(
                        jsonPath("$.results[0].inactiveReason.mergeDemergeTo", contains("P21802")));

        // when default search returns only itself
        response =
                getMockMvc()
                        .perform(
                                get(SEARCH_RESOURCE + "?query=Q14301_FGFR2")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.results.*.primaryAccession", contains("Q14301")));
    }

    @Nested
    class TRM_1234 {
        @ParameterizedTest
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
                                    get(String.format("%s?query=%s", SEARCH_RESOURCE, id))
                                            .header(ACCEPT, APPLICATION_JSON_VALUE));
            response.andDo(log())
                    .andExpect(status().is(HttpStatus.OK.value()))
                    .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                    .andExpect(jsonPath("$.results.size()", is(1)))
                    .andExpect(jsonPath("$.results[0].uniProtkbId", is(id)));
        }

        @ParameterizedTest
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
                                    get(String.format("%s?query=%s", SEARCH_RESOURCE, idPart))
                                            .header(ACCEPT, APPLICATION_JSON_VALUE));
            response.andDo(log())
                    .andExpect(status().is(HttpStatus.OK.value()))
                    .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                    .andExpect(jsonPath("$.results.size()", is(1)))
                    .andExpect(jsonPath("$.results[0].uniProtkbId", is(id)));
        }

        @Test
        void cannotSearchForTrEMBLEntryByFirstPartOfID() throws Exception {
            // given
            String idPart = "XXXX";
            String id = idPart + "1_SPECIES";
            UniProtKBEntry trEntry =
                    UniProtKBEntryBuilder.from(
                                    UniProtEntryMocker.create(UniProtEntryMocker.Type.TR))
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
                                    get(String.format("%s?query=%s", SEARCH_RESOURCE, idPart))
                                            .header(ACCEPT, APPLICATION_JSON_VALUE));
            response.andDo(log())
                    .andExpect(status().is(HttpStatus.OK.value()))
                    .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                    .andExpect(jsonPath("$.results.size()", is(0)));
        }

        @ParameterizedTest
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
                                    get(String.format("%s?query=%s", SEARCH_RESOURCE, idPart))
                                            .header(ACCEPT, APPLICATION_JSON_VALUE));
            response.andDo(log())
                    .andExpect(status().is(HttpStatus.OK.value()))
                    .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                    .andExpect(jsonPath("$.results.size()", is(1)))
                    .andExpect(jsonPath("$.results[0].uniProtkbId", is(id)));
        }
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
                                get(SEARCH_RESOURCE + "?query=accession:Q00007")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.results.*.primaryAccession", contains("Q00007")))
                .andExpect(jsonPath("$.results.*.entryType", contains("Inactive")))
                .andExpect(
                        jsonPath(
                                "$.results.*.inactiveReason.inactiveReasonType",
                                contains("DEMERGED")))
                .andExpect(
                        jsonPath(
                                "$.results.*.inactiveReason.mergeDemergeTo",
                                contains(contains("P21802", "P63151"))));

        // when search accession by default field, returns only itself
        response =
                getMockMvc()
                        .perform(
                                get(SEARCH_RESOURCE + "?query=Q00007")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.results.*.primaryAccession", contains("Q00007")));
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
                                get(SEARCH_RESOURCE + "?query=id:FGFR2_HUMAN")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(2)))
                .andExpect(jsonPath("$.results.*.primaryAccession", contains("P21802", "Q00007")));

        // when search id by default field, it returns only the active ID
        response =
                getMockMvc()
                        .perform(
                                get(SEARCH_RESOURCE + "?query=FGFR2_HUMAN")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.results.*.primaryAccession", contains("P21802")));
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
                                get(SEARCH_RESOURCE + "?query=accession:I8FBX2")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.*.primaryAccession", contains("I8FBX2")))
                .andExpect(jsonPath("$.results.*.entryType", contains("Inactive")))
                .andExpect(
                        jsonPath(
                                "$.results.*.inactiveReason.inactiveReasonType",
                                contains("DELETED")));

        // when search accession by default field, returns only itself
        response =
                getMockMvc()
                        .perform(
                                get(SEARCH_RESOURCE + "?query=I8FBX2")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.*.primaryAccession", contains("I8FBX2")));
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
                                get(SEARCH_RESOURCE + "?query=id:I8FBX2_YERPE")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.*.primaryAccession", contains("I8FBX2")))
                .andExpect(jsonPath("$.results.*.entryType", contains("Inactive")))
                .andExpect(
                        jsonPath(
                                "$.results.*.inactiveReason.inactiveReasonType",
                                contains("DELETED")));

        // when search accession by default field, returns only itself
        response =
                getMockMvc()
                        .perform(
                                get(SEARCH_RESOURCE + "?query=I8FBX2_YERPE")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.*.primaryAccession", contains("I8FBX2")));
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
                                get(SEARCH_RESOURCE
                                                + "?query=Familial&fields=accession,gene_primary&showSingleTermMatchedFields=true")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.matchedFields.size()", is(1)))
                .andExpect(jsonPath("$.matchedFields.*.name", contains("cc_disease")))
                .andExpect(jsonPath("$.matchedFields.*.hits", contains(1)));
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
                                get(SEARCH_RESOURCE
                                                + "?query=gene:FGFR2&fields=accession,gene_primary&showSingleTermMatchedFields=true")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.*.primaryAccession", contains("P21802")))
                .andExpect(jsonPath("$.matchedFields").doesNotExist());
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
                                get(SEARCH_RESOURCE
                                                + "?query=Fibroblast growth&fields=accession,gene_primary&showSingleTermMatchedFields=true")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.*.primaryAccession", contains("P21802")))
                .andExpect(jsonPath("$.matchedFields").doesNotExist());
    }

    @Test
    void cannotReturnMatchedFieldsForXML() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        String acc = entry.getPrimaryAccession().getValue();
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(SEARCH_RESOURCE
                                                + "?query=Familial&fields=accession,gene_primary&showSingleTermMatchedFields=true")
                                        .header(ACCEPT, APPLICATION_XML_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_XML_VALUE))
                .andExpect(
                        content()
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
                                get(SEARCH_RESOURCE)
                                        .param("query", "PR:P21802")
                                        .param("fields", "accession")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.*.primaryAccession", contains("P21802")));
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
                                get(SEARCH_RESOURCE)
                                        .param("query", "HGNC:3689 AND accession:P21802")
                                        .param("fields", "accession,cc_interaction")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.*.primaryAccession", contains("P21802")))
                .andExpect(
                        jsonPath(
                                "$.results.*.comments[0].interactions[0].organismDiffer",
                                contains(true)))
                .andExpect(
                        jsonPath(
                                "$.results.*.comments[0].interactions[0].interactantOne.uniProtKBAccession",
                                contains("P21802")))
                .andExpect(
                        jsonPath(
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
                                get(SEARCH_RESOURCE)
                                        .param("query", "GO:0016020")
                                        .param("fields", "accession")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.*.primaryAccession", contains("P21802")));
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
                                get(SEARCH_RESOURCE)
                                        .param("query", "Reactome NOT(organism_id:9606)")
                                        .param("fields", "accession")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.results.*.primaryAccession", contains("F1Q0X3")));
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
                                get(SEARCH_RESOURCE)
                                        .param("query", "Reactome -organism_id:9606")
                                        .param("fields", "accession")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.results.*.primaryAccession", contains("F1Q0X3")));
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
                                get(SEARCH_RESOURCE)
                                        .param("query", "Reactome !organism_id:9606")
                                        .param("fields", "accession")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.results.*.primaryAccession", contains("F1Q0X3")));
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
                                get(SEARCH_RESOURCE)
                                        .param("query", "adiponectin")
                                        .param("fields", "accession")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.results.*.primaryAccession", contains("P21802")));
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
            switch (searchField) {
                case "accession":
                case "accession_id":
                    value = "P21802";
                    break;
                case "organism_id":
                case "virus_host_id":
                case "taxonomy_id":
                    value = "9606";
                    break;
                case "length":
                case "mass":
                    value = "[* TO *]";
                    break;
                case "proteome":
                    value = "UP000000000";
                    break;
                case "annotation_score":
                    value = "5";
                    break;
                case "date_modified":
                case "date_created":
                case "date_sequence_modified":
                case "lit_pubdate":
                    String now = Instant.now().toString();
                    value = "[* TO " + now + "]";
                    break;
            }
        }
        return value;
    }

    @Override
    protected List<String> getAllFacetFields() {
        return new ArrayList<>(facetConfig.getFacetNames());
    }

    @Override
    protected void saveEntry(SaveScenario saveContext) {
        UniProtKBEntry entry =
                UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL); // P21802
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP); // Q8DIA7
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_ISOFORM); // P21802-2
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        if (SaveScenario.SEARCH_ALL_FIELDS.equals(saveContext)
                || SaveScenario.SEARCH_ALL_RETURN_FIELDS.equals(saveContext)
                || SaveScenario.FACETS_SUCCESS.equals(saveContext)) {
            UniProtDocument doc = new UniProtDocument();
            doc.accession = "P00001";
            doc.active = true;
            doc.isIsoform = false;
            doc.otherOrganism = "Search All";
            doc.organismHostIds.add(9606);
            doc.organismHostNames.add("Search All");
            doc.organelles.add("Search All");
            doc.rcPlasmid.add("Search All");
            doc.rcTransposon.add("Search All");
            doc.rcStrain.add("Search All");
            doc.rcTissue.add("Search All");
            doc.subcellLocationNote.add("Search All");
            doc.subcellLocationNoteEv.add("Search All");
            doc.subcellLocationTerm.add("Search All");
            doc.subcellLocationTermEv.add("Search All");
            doc.ap.add("Search All");
            doc.apEv.add("Search All");
            doc.apAi.add("Search All");
            doc.apAiEv.add("Search All");
            doc.apApu.add("Search All");
            doc.apApuEv.add("Search All");
            doc.apAs.add("Search All");
            doc.apAsEv.add("Search All");
            doc.apRf.add("Search All");
            doc.apRfEv.add("Search All");
            doc.bpcp.add("Search All");
            doc.bpcpEv.add("Search All");
            doc.bpcpAbsorption.add("Search All");
            doc.bpcpAbsorptionEv.add("Search All");
            doc.bpcpKinetics.add("Search All");
            doc.bpcpKineticsEv.add("Search All");
            doc.bpcpPhDependence.add("Search All");
            doc.bpcpPhDependenceEv.add("Search All");
            doc.bpcpRedoxPotential.add("Search All");
            doc.bpcpRedoxPotentialEv.add("Search All");
            doc.bpcpTempDependence.add("Search All");
            doc.bpcpTempDependenceEv.add("Search All");
            doc.inchikey.add("Search All");
            doc.rheaIds.add("Search All");
            doc.chebi.add("Search All");
            doc.cofactorChebi.add("Search All");
            doc.cofactorChebiEv.add("Search All");
            doc.cofactorNote.add("Search All");
            doc.cofactorNoteEv.add("Search All");
            doc.seqCaution.add("Search All");
            doc.seqCautionEv.add("Search All");
            doc.seqCautionFrameshift.add("Search All");
            doc.seqCautionErInit.add("Search All");
            doc.seqCautionErPred.add("Search All");
            doc.seqCautionErTerm.add("Search All");
            doc.seqCautionErTran.add("Search All");
            doc.seqCautionMisc.add("Search All");
            doc.seqCautionMiscEv.add("Search All");
            doc.proteomes.add("UP000000000");
            doc.uniparc = "UPI000000000";
            doc.unirefCluster50 = "UniRef50_P0001";
            doc.unirefCluster90 = "UniRef90_P0001";
            doc.unirefCluster100 = "UniRef100_P0001";
            doc.computationalPubmedIds.add("890123456");
            doc.communityPubmedIds.add("1234567");
            UniProtDatabaseTypes.INSTANCE
                    .getAllDbTypes()
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
                                doc.featureEvidenceMap.put(
                                        "ftev_" + typeName, Collections.singleton("Search All"));
                                doc.featureLengthMap.put(
                                        "ftlen_" + typeName, Collections.singleton(10));
                            });

            Arrays.stream(FeatureCategory.values())
                    .map(type -> type.getName().toLowerCase())
                    .forEach(
                            type -> {
                                doc.featuresMap.put(
                                        "ft_" + type, Collections.singleton("Search All"));
                                doc.featureEvidenceMap.put(
                                        "ftev_" + type, Collections.singleton("Search All"));
                                doc.featureLengthMap.put(
                                        "ftlen_" + type, Collections.singleton(10));
                            });

            Arrays.stream(CommentType.values())
                    .forEach(
                            type -> {
                                String typeName = type.name().toLowerCase();
                                doc.commentMap.put(
                                        "cc_" + typeName, Collections.singleton("Search All"));
                                doc.commentEvMap.put(
                                        "ccev_" + typeName, Collections.singleton("Search All"));
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

    static class UniprotKBSearchParameterResolver extends AbstractSearchParameterResolver {

        @Override
        protected SearchParameter searchCanReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("accession:P21802"))
                    .resultMatcher(jsonPath("$.results.*.primaryAccession", contains("P21802")))
                    .build();
        }

        @Override
        protected SearchParameter searchCanReturnNotFoundParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("accession:P12345"))
                    .resultMatcher(jsonPath("$.results.size()", is(0)))
                    .build();
        }

        @Override
        protected SearchParameter searchAllowWildcardQueryAllDocumentsParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("organism_name:*"))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.primaryAccession",
                                    contains(ACCESSION_SP_CANONICAL, ACCESSION_SP)))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.organism.taxonId",
                                    containsInAnyOrder(9606, 197221)))
                    .build();
        }

        @Override
        protected SearchParameter searchQueryWithInvalidTypeQueryReturnBadRequestParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("gene:[1 TO 10]"))
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
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
                                            + "OR is_isoform:invalid OR structure_3d:invalid OR active:invalid OR proteome:INVALID"))
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    containsInAnyOrder(
                                            "The 'accession' filter value 'INVALID' has invalid format. It should be a valid UniProtKB accession",
                                            "The 'proteome' filter value has invalid format. It should match the regular expression UP[0-9]{9}",
                                            "The 'is_isoform' filter value can only be true or false",
                                            "The 'reviewed' filter value can only be true or false",
                                            "The 'active' parameter can only be true or false",
                                            "The 'organism_id' filter value should be a number",
                                            "The 'structure_3d' filter value can only be true or false",
                                            "The 'taxonomy_id' filter value should be a number",
                                            "The 'accession_id' filter value 'INVALID' has invalid format. It should be a valid UniProtKB accession",
                                            "The 'virus_host_id' filter value should be a number")))
                    .build();
        }

        @Override
        protected SearchParameter searchSortWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("sort", Collections.singletonList("gene desc"))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.primaryAccession",
                                    contains(ACCESSION_SP, ACCESSION_SP_CANONICAL)))
                    .resultMatcher(
                            jsonPath(
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
                            jsonPath(
                                    "$.results.*.primaryAccession",
                                    contains(ACCESSION_SP_CANONICAL, ACCESSION_SP)))
                    .resultMatcher(jsonPath("$.results.*.proteinDescription").exists())
                    .resultMatcher(jsonPath("$.results.*.genes").exists())
                    .resultMatcher(jsonPath("$.results.*.comments").doesNotExist())
                    .resultMatcher(jsonPath("$.results.*.features").doesNotExist())
                    .resultMatcher(jsonPath("$.results.*.keywords").doesNotExist())
                    .resultMatcher(jsonPath("$.results.*.references").doesNotExist())
                    .resultMatcher(jsonPath("$.results.*.sequence").doesNotExist())
                    .build();
        }

        @Override
        protected SearchParameter searchFacetsWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("facets", Collections.singletonList("reviewed,fragment"))
                    .resultMatcher(
                            jsonPath(
                                    "$.results.*.primaryAccession",
                                    contains(ACCESSION_SP_CANONICAL, ACCESSION_SP, "P00001")))
                    .resultMatcher(jsonPath("$.facets", notNullValue()))
                    .resultMatcher(jsonPath("$.facets", not(empty())))
                    .resultMatcher(
                            jsonPath("$.facets.*.name", containsInAnyOrder("reviewed", "fragment")))
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
                                            jsonPath(
                                                    "$.results.*.primaryAccession",
                                                    contains(ACCESSION_SP_CANONICAL, ACCESSION_SP)))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_XML)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            startsWith(
                                                                    XML_DECLARATION
                                                                            + UNIPROTKB_XML_SCHEMA)))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "<accession>P21802</accession>")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "<accession>Q8DIA7</accession>")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            endsWith(
                                                                    COPYRIGHT_TAG
                                                                            + UNIPROTKB_XML_CLOSE_TAG)))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.FF_MEDIA_TYPE)
                                    .resultMatcher(content().string(containsString("AC   P21802;")))
                                    .resultMatcher(content().string(containsString("AC   Q8DIA7;")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.FASTA_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    ">sp|Q8DIA7|"
                                                                            + "PURL_THEEB Phosphoribosylformylglycinamidine synthase subunit PurL "
                                                                            + "OS=Thermosynechococcus elongatus (strain BP-1) OX=197221 GN=purL PE=3 SV=1")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    ">sp|P21802|FGFR2_HUMAN Fibroblast"
                                                                            + " growth factor receptor 2 OS=Homo sapiens OX=9606 GN=FGFR2 PE=1 SV=1")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.GFF_MEDIA_TYPE)
                                    .resultMatcher(
                                            content().string(containsString("##gff-version 3")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "##sequence-region Q8DIA7 1 761")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "##sequence-region P21802 1 821")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(containsString(ACCESSION_SP_CANONICAL)))
                                    .resultMatcher(content().string(containsString(ACCESSION_SP)))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "Entry\tEntry Name\tReviewed\tProtein names\tGene Names\tOrganism\tLength")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "Q8DIA7\tPURL_THEEB\treviewed\tPhosphoribosylformylglycinamidine synthase subunit PurL, FGAM synthase")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "P21802\tFGFR2_HUMAN\treviewed\tFibroblast growth factor receptor 2, FGFR-2")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                                    .resultMatcher(
                                            content().contentType(UniProtMediaType.XLS_MEDIA_TYPE))
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
                                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.messages.*",
                                                    contains(
                                                            "The 'accession' filter value 'invalid' has invalid format. It should be a valid UniProtKB accession")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_XML)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "<messages>The 'accession' filter value 'invalid' has invalid format. It should be a valid UniProtKB accession</messages>")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.FASTA_MEDIA_TYPE)
                                    .resultMatcher(content().string(isEmptyString()))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.GFF_MEDIA_TYPE)
                                    .resultMatcher(content().string(isEmptyString()))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.FF_MEDIA_TYPE)
                                    .resultMatcher(content().string(isEmptyString()))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(content().string(isEmptyString()))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(content().string(isEmptyString()))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                                    .resultMatcher(content().string(isEmptyString()))
                                    .build())
                    .build();
        }
    }
}
