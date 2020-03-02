package org.uniprot.api.uniprotkb.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.uniprot.api.uniprotkb.controller.UniprotKBController.UNIPROTKB_RESOURCE;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
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
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.api.uniprotkb.UniProtKBREST;
import org.uniprot.api.uniprotkb.repository.DataStoreTestConfig;
import org.uniprot.api.uniprotkb.repository.search.impl.UniprotFacetConfig;
import org.uniprot.api.uniprotkb.repository.search.impl.UniprotQueryRepository;
import org.uniprot.api.uniprotkb.repository.store.UniProtKBStoreClient;
import org.uniprot.core.cv.xdb.UniProtDatabaseAttribute;
import org.uniprot.core.uniprot.UniProtEntry;
import org.uniprot.core.uniprot.UniProtEntryType;
import org.uniprot.core.uniprot.builder.UniProtEntryBuilder;
import org.uniprot.core.uniprot.comment.CommentType;
import org.uniprot.core.uniprot.feature.FeatureCategory;
import org.uniprot.core.uniprot.feature.FeatureType;
import org.uniprot.core.uniprot.xdb.builder.UniProtCrossReferenceBuilder;
import org.uniprot.cv.chebi.ChebiRepo;
import org.uniprot.cv.ec.ECRepo;
import org.uniprot.cv.xdb.UniProtDatabaseImpl;
import org.uniprot.cv.xdb.UniProtXDbTypes;
import org.uniprot.store.datastore.voldemort.uniprot.VoldemortInMemoryUniprotEntryStore;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.indexer.uniprot.inactiveentry.InactiveUniProtEntry;
import org.uniprot.store.indexer.uniprot.mockers.*;
import org.uniprot.store.indexer.uniprotkb.converter.UniProtEntryConverter;
import org.uniprot.store.indexer.uniprotkb.processor.InactiveEntryConverter;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.uniprot.UniProtDocument;
import org.uniprot.store.search.domain.EvidenceGroup;
import org.uniprot.store.search.domain.EvidenceItem;
import org.uniprot.store.search.domain.Field;
import org.uniprot.store.search.domain.impl.GoEvidences;
import org.uniprot.store.search.domain.impl.UniProtResultFields;
import org.uniprot.store.search.domain2.SearchField;
import org.uniprot.store.search.field.UniProtSearchFields;

@ContextConfiguration(
        classes = {DataStoreTestConfig.class, UniProtKBREST.class, ErrorHandlerConfig.class})
@ActiveProfiles(profiles = "offline")
@AutoConfigureWebClient
@WebMvcTest(UniprotKBController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
            UniprotKBSearchControllerIT.UniprotKbSearchParameterResolver.class,
            UniprotKBSearchControllerIT.UniprotKbSearchContentTypeParamResolver.class
        })
@Slf4j
class UniprotKBSearchControllerIT extends AbstractSearchWithFacetControllerIT {

    private static final String SEARCH_RESOURCE = UNIPROTKB_RESOURCE + "/search";

    private static final String ACCESSION_SP_CANONICAL = "P21802";
    private static final String ACCESSION_SP = "Q8DIA7";

    @Autowired private UniprotQueryRepository repository;

    private UniProtKBStoreClient storeClient;

    @Autowired private UniprotFacetConfig facetConfig;

    @BeforeAll
    void initUniprotKbDataStore() {
        DataStoreManager dsm = getStoreManager();
        dsm.addDocConverter(
                DataStoreManager.StoreType.UNIPROT,
                new UniProtEntryConverter(
                        TaxonomyRepoMocker.getTaxonomyRepo(),
                        GoRelationsRepoMocker.getGoRelationRepo(),
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

    @Test
    void searchInvalidIncludeIsoformParameterValue() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
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
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains(
                                        "Invalid includeIsoform parameter value. Expected true or false")));
    }

    @Test
    void searchSecondaryAccession() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
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
                                                + "?query=accession:B4DFC2&fields=accession,gene_primary")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.results.*.primaryAccession", contains("P21802")));
    }

    @Test
    void searchCanonicalOnly() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
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
        response.andDo(print())
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
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
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
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.*.primaryAccession", not("P21802")))
                .andExpect(jsonPath("$.results.*.primaryAccession", contains("P21802-1")))
                .andExpect(jsonPath("$.results.*.primaryAccession", not("P21802-2")));
    }

    @Test
    void searchIncludeCanonicalAndIsoForm() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
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
        response.andDo(print())
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
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
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
        response.andDo(print())
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
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
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
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.*.primaryAccession", not("P21802")))
                .andExpect(jsonPath("$.results.*.primaryAccession", not("P21802-1")))
                .andExpect(jsonPath("$.results.*.primaryAccession", contains("P21802-2")));
    }

    @Test
    void canNotReturnFacetInformationForXML() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
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
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_XML_VALUE))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "Invalid content type received, 'application/xml'. Expected one of [application/json]")));
    }

    @Test
    void searchForMergedInactiveEntriesAlsoReturnsActiveOne() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        List<InactiveUniProtEntry> mergedList =
                InactiveEntryMocker.create(InactiveEntryMocker.InactiveType.MERGED);
        getStoreManager()
                .saveEntriesInSolr(DataStoreManager.StoreType.INACTIVE_UNIPROT, mergedList);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(SEARCH_RESOURCE
                                                + "?query=accession:Q14301&fields=accession,organism")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.results.*.primaryAccession",
                                containsInAnyOrder("P21802", "Q14301")));
    }

    @Test
    void searchForDeMergedInactiveEntriesReturnItself() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        List<InactiveUniProtEntry> demergedList =
                InactiveEntryMocker.create(InactiveEntryMocker.InactiveType.DEMERGED);
        getStoreManager()
                .saveEntriesInSolr(DataStoreManager.StoreType.INACTIVE_UNIPROT, demergedList);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(SEARCH_RESOURCE
                                                + "?query=accession:Q00007&fields=accession,organism")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.*.primaryAccession", contains("Q00007")))
                .andExpect(jsonPath("$.results.*.entryType", contains("Inactive")))
                .andExpect(
                        jsonPath(
                                "$.results.*.inactiveReason.inactiveReasonType",
                                contains("DEMERGED")))
                .andExpect(
                        jsonPath(
                                "$.results.*.inactiveReason.mergeDemergeTo",
                                contains(contains("P63150", "P63151"))));
    }

    @Test
    void searchForDeletedInactiveEntriesReturnItself() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        List<InactiveUniProtEntry> deletedList =
                InactiveEntryMocker.create(InactiveEntryMocker.InactiveType.DELETED);
        getStoreManager()
                .saveEntriesInSolr(DataStoreManager.StoreType.INACTIVE_UNIPROT, deletedList);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(SEARCH_RESOURCE
                                                + "?query=mnemonic:I8FBX2_YERPE&fields=accession,organism")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.*.primaryAccession", contains("I8FBX2")))
                .andExpect(jsonPath("$.results.*.entryType", contains("Inactive")))
                .andExpect(
                        jsonPath(
                                "$.results.*.inactiveReason.inactiveReasonType",
                                contains("DELETED")));
    }

    @Test
    void defaultSearchWithMatchedFields() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
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
                                                + "?query=Familial&fields=accession,gene_primary&showMatchedFields=true")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.matchedFields.size()", is(1)))
                .andExpect(jsonPath("$.matchedFields.*.name", contains("cc_disease")))
                .andExpect(jsonPath("$.matchedFields.*.hits", contains(1)));
    }

    @Test
    void badDefaultSearchWithMatchedFields() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(SEARCH_RESOURCE
                                                + "?query=gene:Fibroblast&fields=accession,gene_primary&showMatchedFields=true")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "Term information will only be returned for single value searches that do not specify a field")));
    }

    @Test
    void cannotReturnMatchedFieldsForXML() throws Exception {
        // given
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        String acc = entry.getPrimaryAccession().getValue();
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(SEARCH_RESOURCE
                                                + "?query=Familial&fields=accession,gene_primary&showMatchedFields=true")
                                        .header(ACCEPT, APPLICATION_XML_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_XML_VALUE))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "Invalid content type received, 'application/xml'. Expected one of [application/json]")));
    }

    @Override
    protected String getSearchRequestPath() {
        return SEARCH_RESOURCE;
    }

    @Override
    protected int getDefaultPageSize() {
        return 25;
    }

    @Override
    protected Collection<String> getAllSearchFields() {
        return UniProtSearchFields.UNIPROTKB.getSearchFields().stream()
                .map(SearchField::getName)
                .collect(Collectors.toSet());
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
                case "host_id":
                case "taxonomy_id":
                    value = "9606";
                    break;
                case "modified":
                case "created":
                case "sequence_modified":
                case "lit_pubdate":
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
            }
        }
        return value;
    }

    @Override
    protected boolean fieldValueIsValid(String field, String value) {
        return UniProtSearchFields.UNIPROTKB.fieldValueIsValid(field, value);
    }

    @Override
    protected Collection<String> getAllSortFields() {
        return UniProtSearchFields.UNIPROTKB.getSearchFields().stream()
                .filter(field -> field.getSortField().isPresent())
                .map(SearchField::getName)
                .collect(Collectors.toList());
    }

    @Override
    protected List<String> getAllFacetFields() {
        return new ArrayList<>(facetConfig.getFacetNames());
    }

    @Override
    protected List<String> getAllReturnedFields() {
        return UniProtResultFields.INSTANCE.getResultFields().stream()
                .flatMap(fieldGroup -> fieldGroup.getFields().stream().map(Field::getName))
                .collect(Collectors.toList());
    }

    @Override
    protected void saveEntry(SaveScenario saveContext) {
        UniProtEntry entry =
                UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL); // P21802
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP); // Q8DIA7
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_ISOFORM); // P21802-2
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        if (SaveScenario.SEARCH_ALL_FIELDS.equals(saveContext)
                || SaveScenario.SEARCH_ALL_RETURN_FIELDS.equals(saveContext)) {
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

            UniProtXDbTypes.INSTANCE.getAllDBXRefTypes().stream()
                    .map(db -> db.getName().toLowerCase())
                    .forEach(dbName -> doc.xrefCountMap.put("xref_count_" + dbName, 0L));

            Arrays.stream(FeatureType.values())
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
                    .map(type -> type.name().toLowerCase())
                    .forEach(
                            type -> {
                                doc.commentMap.put(
                                        "cc_" + type, Collections.singleton("Search All"));
                                doc.commentEvMap.put(
                                        "ccev_" + type, Collections.singleton("Search All"));
                            });

            List<String> goAssertionCodes =
                    GoEvidences.INSTANCE.getEvidences().stream()
                            .filter(this::getManualEvidenceGroup)
                            .flatMap(this::getEvidenceCodes)
                            .map(String::toLowerCase)
                            .collect(Collectors.toList());

            goAssertionCodes.addAll(
                    Arrays.asList(
                            "rca", "nd", "ibd", "ikr", "ird", "unknown")); // TODO: is it correct?

            goAssertionCodes.forEach(
                    code ->
                            doc.goWithEvidenceMaps.put(
                                    "go_" + code, Collections.singleton("Search All")));

            entry =
                    new UniProtEntryBuilder("P00001", "ID_P00001", UniProtEntryType.SWISSPROT)
                            .databaseCrossReferencesAdd(
                                    new UniProtCrossReferenceBuilder()
                                            .databaseType(new UniProtDatabaseImpl("Proteomes"))
                                            .id("UP000000000")
                                            .propertiesAdd(
                                                    new UniProtDatabaseAttribute("a", "a", "a"),
                                                    "value")
                                            .build())
                            .build();

            getStoreManager().saveDocs(DataStoreManager.StoreType.UNIPROT, doc);
            getStoreManager().saveToStore(DataStoreManager.StoreType.UNIPROT, entry);
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

                            UniProtEntry entry =
                                    new UniProtEntryBuilder(
                                                    "P0000" + i,
                                                    "P12345_ID",
                                                    UniProtEntryType.TREMBL)
                                            .build();

                            getStoreManager().saveDocs(DataStoreManager.StoreType.UNIPROT, doc);
                            getStoreManager()
                                    .saveToStore(DataStoreManager.StoreType.UNIPROT, entry);
                        });
    }

    static class UniprotKbSearchParameterResolver extends AbstractSearchParameterResolver {

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
                    .queryParam("query", Collections.singletonList("organism:*"))
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
                                            + "OR reviewed:INVALID OR organism_id:invalid OR host_id:invalid OR taxonomy_id:invalid "
                                            + "OR is_isoform:invalid OR d3structure:invalid OR active:invalid OR proteome:INVALID"))
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
                                            "The 'd3structure' filter value can only be true or false",
                                            "The 'taxonomy_id' filter value should be a number",
                                            "The 'accession_id' filter value 'INVALID' has invalid format. It should be a valid UniProtKB accession",
                                            "The 'host_id' filter value should be a number")))
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
                    .queryParam("fields", Collections.singletonList("gene_primary,protein_name"))
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
                                    contains(ACCESSION_SP_CANONICAL, ACCESSION_SP)))
                    .resultMatcher(jsonPath("$.facets", notNullValue()))
                    .resultMatcher(jsonPath("$.facets", not(empty())))
                    .resultMatcher(jsonPath("$.facets.*.name", contains("reviewed", "fragment")))
                    .build();
        }
    }

    static class UniprotKbSearchContentTypeParamResolver
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
                                                            containsString(
                                                                    "<accession>P21802</accession>")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "<accession>Q8DIA7</accession>")))
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
