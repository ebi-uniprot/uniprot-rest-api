package org.uniprot.api.uniprotkb.controller;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.uniprot.api.rest.output.UniProtMediaType.*;
import static org.uniprot.api.rest.output.converter.ConverterConstants.*;
import static org.uniprot.api.uniprotkb.controller.UniProtKBController.UNIPROTKB_RESOURCE;
import static org.uniprot.store.indexer.uniprot.mockers.InactiveEntryMocker.MERGED;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.rest.controller.AbstractGetByIdWithTypeExtensionControllerIT;
import org.uniprot.api.rest.controller.param.ContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdParameter;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetIdContentTypeParamResolver;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetIdParameterResolver;
import org.uniprot.api.rest.download.AsyncDownloadMocks;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.service.NTriplesPrologs;
import org.uniprot.api.rest.service.RdfPrologs;
import org.uniprot.api.rest.service.TurtlePrologs;
import org.uniprot.api.uniprotkb.UniProtKBREST;
import org.uniprot.api.uniprotkb.repository.DataStoreTestConfig;
import org.uniprot.api.uniprotkb.repository.search.impl.UniprotQueryRepository;
import org.uniprot.api.uniprotkb.repository.store.UniProtKBStoreClient;
import org.uniprot.api.uniprotkb.service.UniSaveClient;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.uniprotkb.impl.UniProtKBEntryBuilder;
import org.uniprot.store.datastore.voldemort.uniprot.VoldemortInMemoryUniprotEntryStore;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.indexer.uniprot.inactiveentry.InactiveUniProtEntry;
import org.uniprot.store.indexer.uniprot.mockers.InactiveEntryMocker;
import org.uniprot.store.indexer.uniprot.mockers.UniProtEntryMocker;
import org.uniprot.store.indexer.uniprotkb.processor.InactiveEntryConverter;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.spark.indexer.uniprot.converter.UniProtEntryConverter;

/** @author lgonzales */
@ContextConfiguration(
        classes = {DataStoreTestConfig.class, AsyncDownloadMocks.class, UniProtKBREST.class})
@ActiveProfiles(profiles = "offline")
@AutoConfigureWebClient
@WebMvcTest(UniProtKBController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
            UniProtKBByAccessionControllerIT.UniprotKBGetIdParameterResolver.class,
            UniProtKBByAccessionControllerIT.UniprotKBGetIdContentTypeParamResolver.class,
            MockitoExtension.class
        })
class UniProtKBByAccessionControllerIT extends AbstractGetByIdWithTypeExtensionControllerIT {

    private static final String ACCESSION_RESOURCE = UNIPROTKB_RESOURCE + "/{accession}";

    private static final String ACCESSION_ID = "Q8DIA7";

    @Autowired private UniprotQueryRepository repository;

    private UniProtKBStoreClient storeClient;

    @MockBean(name = "uniProtRdfRestTemplate")
    private RestTemplate restTemplate;

    @Autowired private UniSaveClient uniSaveClient;

    public Stream<Arguments> fetchingInactiveEntriesWithFileExtension() {
        return Stream.of(
                Arguments.of(
                        getFileExtension(APPLICATION_JSON), "P00000", "P99999", "MY_ID", "MY_ID"),
                Arguments.of(getFileExtension(FF_MEDIA_TYPE), "P00000", "P99999", "MY_ID", "MY_ID"),
                Arguments.of(
                        getFileExtension(GFF_MEDIA_TYPE), "P00000", "P99999", "MY_ID", "P99999"),
                Arguments.of(
                        getFileExtension(LIST_MEDIA_TYPE), "P00000", "P99999", "MY_ID", "P99999"),
                Arguments.of(
                        getFileExtension(TSV_MEDIA_TYPE), "P00000", "P99999", "MY_ID", "P99999"),
                Arguments.of(
                        getFileExtension(APPLICATION_XML), "P00000", "P99999", "MY_ID", "P99999"),
                Arguments.of(
                        getFileExtension(XLS_MEDIA_TYPE), "P00000", "P99999", "MY_ID", "BINARY"),
                Arguments.of(
                        getFileExtension(FASTA_MEDIA_TYPE), "P00000", "P99999", "MY_ID", "P99999"),
                Arguments.of(
                        getFileExtension(RDF_MEDIA_TYPE), "P00000", "P99999", "MY_ID", "P00000"),
                Arguments.of(
                        getFileExtension(TURTLE_MEDIA_TYPE), "P00000", "P99999", "MY_ID", "P00000"),
                Arguments.of(
                        getFileExtension(N_TRIPLES_MEDIA_TYPE),
                        "P00000",
                        "P99999",
                        "MY_ID",
                        "P00000"));
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

    @Override
    protected void saveEntry() {
        initUniprotKbDataStore();
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);
    }

    @Override
    protected String getIdRequestPath() {
        return ACCESSION_RESOURCE;
    }

    void initUniprotKbDataStore() {
        UniProtEntryConverter uniProtEntryConverter = new UniProtEntryConverter(new HashMap<>());

        DataStoreManager dsm = getStoreManager();
        dsm.addDocConverter(DataStoreManager.StoreType.UNIPROT, uniProtEntryConverter);
        dsm.addDocConverter(
                DataStoreManager.StoreType.INACTIVE_UNIPROT, new InactiveEntryConverter());
        dsm.addSolrClient(DataStoreManager.StoreType.INACTIVE_UNIPROT, SolrCollection.uniprot);

        storeClient =
                new UniProtKBStoreClient(
                        VoldemortInMemoryUniprotEntryStore.getInstance("avro-uniprot"));
        dsm.addStore(DataStoreManager.StoreType.UNIPROT, storeClient);
    }

    @Test
    void invalidMultipleFieldsParametersFromAccessionEndpointReturnBadRequest() throws Exception {
        // given
        saveEntry();

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(ACCESSION_RESOURCE, ACCESSION_ID)
                                        .param("fields", "invalid, organism_name, invalid2")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                containsInAnyOrder(
                                        "Invalid fields parameter value 'invalid'",
                                        "Invalid fields parameter value 'invalid2'")));
    }

    @Test
    void canSearchIsoFormEntryFromAccessionEndpoint() throws Exception {
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
                                get(ACCESSION_RESOURCE, "P21802-2")
                                        .param("fields", "accession,organism_name")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.primaryAccession", is("P21802-2")))
                .andExpect(jsonPath("$.organism").exists())
                // ensure other parts of the entry were not returned (using one example)
                .andExpect(jsonPath("$.genes").doesNotExist());
    }

    @Test
    void canSearchCanonicalIsoFormEntryFromAccessionEndpoint() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL_ISOFORM);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_ISOFORM);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        // when is an accession that has isoforms (alternative products comment)
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(ACCESSION_RESOURCE, "P21802-1")
                                        .param("fields", "accession,organism_name")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then return the canonical isoform entry
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.primaryAccession", is("P21802-1")))
                .andExpect(jsonPath("$.organism").exists())
                // ensure other parts of the entry were not returned (using one example)
                .andExpect(jsonPath("$.comments").doesNotExist());

        // when is an accession without isoforms (Alternative products comment)
        response =
                getMockMvc()
                        .perform(
                                get(ACCESSION_RESOURCE, "q8dia7-1")
                                        .param("fields", "accession,organism_name")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then return the canonical entry
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.primaryAccession", is("Q8DIA7")));
    }

    @Test
    void withMergedInactiveEntryReturnTheActiveOne() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        List<InactiveUniProtEntry> mergedList =
                InactiveEntryMocker.create(InactiveEntryMocker.InactiveType.MERGED);
        getStoreManager()
                .saveEntriesInSolr(DataStoreManager.StoreType.INACTIVE_UNIPROT, mergedList);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(ACCESSION_RESOURCE, "B4DFC2")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.SEE_OTHER.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string(HttpHeaders.LOCATION, "/uniprotkb/P21802?from=B4DFC2"))
                .andExpect(jsonPath("$.primaryAccession", is("B4DFC2")))
                .andExpect(jsonPath("$.entryType", is("Inactive")))
                .andExpect(jsonPath("$.inactiveReason.inactiveReasonType", is("MERGED")))
                .andExpect(jsonPath("$.inactiveReason.mergeDemergeTo", contains("P21802")));
    }

    @ParameterizedTest
    @MethodSource("fetchingInactiveEntriesWithFileExtension")
    void inactiveEntryWithExtensionReturnsActiveOneWithExtension(
            String fileExtension,
            String inactiveAcc,
            String activeAcc,
            String activeId,
            String expectResponseMatch)
            throws Exception {
        // GIVEN
        MediaType mediaType = UniProtMediaType.getMediaTypeForFileExtension(fileExtension);
        UniProtKBEntry template = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        UniProtKBEntry activeEntry =
                UniProtKBEntryBuilder.from(template)
                        .primaryAccession(activeAcc)
                        .uniProtId(activeId)
                        .build();
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, activeEntry);

        // ... inactive entry that was merged into 'activeAcc' entry
        InactiveUniProtEntry inactiveEntry =
                InactiveUniProtEntry.from(inactiveAcc, "I8FBX0_MYCAB", MERGED, activeAcc);
        getStoreManager()
                .saveEntriesInSolr(DataStoreManager.StoreType.INACTIVE_UNIPROT, inactiveEntry);

        // REQUEST 1 ---------------------------
        // WHEN we fetch the inactive accession
        ResultActions response =
                getMockMvc().perform(get(ACCESSION_RESOURCE, inactiveAcc + "." + fileExtension));

        // THEN we expect a redirect 303 (200 for RDF)
        ResultActions resultActions = response.andDo(log());
        String redirectedURL = "/uniprotkb/" + activeAcc;
        if (!mediaType.equals(DEFAULT_MEDIA_TYPE)) {
            redirectedURL += "." + fileExtension;
        }
        redirectedURL += "?from=" + inactiveAcc;

        if (Set.of(RDF_MEDIA_TYPE, TURTLE_MEDIA_TYPE, N_TRIPLES_MEDIA_TYPE).contains(mediaType)) {
            resultActions
                    .andExpect(status().is(HttpStatus.OK.value()))
                    .andExpect(header().string(HttpHeaders.CONTENT_TYPE, mediaType.toString()));
        } else {
            // response REDIRECTS client
            resultActions
                    .andExpect(status().is(HttpStatus.SEE_OTHER.value()))
                    .andExpect(header().string(HttpHeaders.CONTENT_TYPE, mediaType.toString()))
                    .andExpect(header().string(HttpHeaders.LOCATION, redirectedURL));

            // REQUEST 2 ---------------------------
            // ... let's simulate a client (e.g., browser) redirect and go to the address directly
            response = getMockMvc().perform(get(redirectedURL));

            // then we expect the active entry to contain the appropriate contents
            Matcher<String> matcher = containsString(expectResponseMatch);
            if (expectResponseMatch.equals("BINARY")) {
                matcher = is(not(emptyString()));
            }

            response.andDo(log())
                    .andExpect(status().is(HttpStatus.OK.value()))
                    .andExpect(header().string(HttpHeaders.CONTENT_TYPE, mediaType.toString()))
                    .andExpect(content().string(matcher));
        }
    }

    @Test
    void searchForDeMergedInactiveEntriesReturnItself() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        List<InactiveUniProtEntry> demergedList =
                InactiveEntryMocker.create(InactiveEntryMocker.InactiveType.DEMERGED);
        getStoreManager()
                .saveEntriesInSolr(DataStoreManager.StoreType.INACTIVE_UNIPROT, demergedList);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(ACCESSION_RESOURCE, "Q00007")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.primaryAccession", is("Q00007")))
                .andExpect(jsonPath("$.entryType", is("Inactive")))
                .andExpect(jsonPath("$.inactiveReason.inactiveReasonType", is("DEMERGED")))
                .andExpect(
                        jsonPath("$.inactiveReason.mergeDemergeTo", contains("P21802", "P63151")));
    }

    @Test
    void searchForDeletedInactiveEntriesReturnItself() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        List<InactiveUniProtEntry> deletedList =
                InactiveEntryMocker.create(InactiveEntryMocker.InactiveType.DELETED);
        getStoreManager()
                .saveEntriesInSolr(DataStoreManager.StoreType.INACTIVE_UNIPROT, deletedList);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(ACCESSION_RESOURCE, "I8FBX2")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.primaryAccession", is("I8FBX2")))
                .andExpect(jsonPath("$.entryType", is("Inactive")))
                .andExpect(jsonPath("$.inactiveReason.inactiveReasonType", is("DELETED")));
    }

    @Test
    void searchWithAccessionVersionEntriesRedirectToAccession() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(ACCESSION_RESOURCE, "P21802.20")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.SEE_OTHER.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(
                        header().string(
                                        HttpHeaders.LOCATION,
                                        "/unisave/P21802?from=P21802.20&versions=20&format=json"));
    }

    @Test
    void searchWithIDForActiveEntriesRedirectToAccession() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(ACCESSION_RESOURCE, "FGFR2_HUMAN")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.SEE_OTHER.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(
                        header().string(
                                        HttpHeaders.LOCATION,
                                        "/uniprotkb/P21802?from=FGFR2_HUMAN"));
    }

    @Test
    void searchWithIDResourceNotFound() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(ACCESSION_RESOURCE, "FGFR2_DOG")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(header().string(HttpHeaders.LOCATION, nullValue()));
    }

    @Test
    void searchWithIDForDeMergedInactiveEntriesRedirectToAccession() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        List<InactiveUniProtEntry> mergedList =
                InactiveEntryMocker.create(InactiveEntryMocker.InactiveType.DEMERGED);
        getStoreManager()
                .saveEntriesInSolr(DataStoreManager.StoreType.INACTIVE_UNIPROT, mergedList);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(ACCESSION_RESOURCE, "FGFR2_HUMAN")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.SEE_OTHER.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(
                        header().string(
                                        HttpHeaders.LOCATION,
                                        "/uniprotkb/P21802?from=FGFR2_HUMAN"));
    }

    @Test
    void searchWithIDForMergedInactiveEntriesRedirectToAccession() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        List<InactiveUniProtEntry> mergedList =
                InactiveEntryMocker.create(InactiveEntryMocker.InactiveType.MERGED);
        getStoreManager()
                .saveEntriesInSolr(DataStoreManager.StoreType.INACTIVE_UNIPROT, mergedList);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(ACCESSION_RESOURCE, "Q14301_FGFR2")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.SEE_OTHER.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(
                        header().string(
                                        HttpHeaders.LOCATION,
                                        "/uniprotkb/Q14301?from=Q14301_FGFR2"));
    }

    @Test
    void searchWithIDForDeletedInactiveEntriesRedirectToAccession() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        List<InactiveUniProtEntry> deletedList =
                InactiveEntryMocker.create(InactiveEntryMocker.InactiveType.DELETED);
        getStoreManager()
                .saveEntriesInSolr(DataStoreManager.StoreType.INACTIVE_UNIPROT, deletedList);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(ACCESSION_RESOURCE, "I8FBX2_YERPE")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.SEE_OTHER.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                .andExpect(
                        header().string(
                                        HttpHeaders.LOCATION,
                                        "/uniprotkb/I8FBX2?from=I8FBX2_YERPE"));
    }

    @Test
    void searchAccessionLastVersionFromUnisave() throws Exception {
        String SAMPLE_ENTRY_VERSION_HISTORY_RESPONSE =
                "{'results':[{'accession':'A0A1J4H6S2','database':'TrEMBL','entryVersion':9,'firstRelease':'2021_02/2021_02','firstReleaseDate':'07-Apr-2021','lastRelease':'2022_01/2022_01','lastReleaseDate':'23-Feb-2022','name':'A0A1J4H6S2_9STAP','sequenceVersion':1}]}";

        when(uniSaveClient.getUniSaveHistoryVersion(Mockito.any()))
                .thenReturn(SAMPLE_ENTRY_VERSION_HISTORY_RESPONSE);
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(ACCESSION_RESOURCE, "A0A1J4H6S2")
                                        .param("version", "last")
                                        .header(ACCEPT, FF_MEDIA_TYPE_VALUE));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.SEE_OTHER.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, FF_MEDIA_TYPE_VALUE))
                .andExpect(
                        header().string(
                                        HttpHeaders.LOCATION,
                                        getRedirectToEntryVersionPath("A0A1J4H6S2", "9", "txt")));
    }

    @Test
    void uniSaveEntryHistoryEndpointWithAccessionNotPresent() throws Exception {
        String SAMPLE_ENTRY_VERSION_HISTORY_NOT_FOUND =
                "{'url':'http://rest.uniprot.org/unisave/B0B1J1H6A7','messages':['No entries for B0B1J1H6A7 were found']}";

        when(uniSaveClient.getUniSaveHistoryVersion(Mockito.any()))
                .thenReturn(SAMPLE_ENTRY_VERSION_HISTORY_NOT_FOUND);
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(ACCESSION_RESOURCE, "B0B1J1H6A7")
                                        .param("version", "last")
                                        .header(ACCEPT, FF_MEDIA_TYPE_VALUE));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(
                        result ->
                                assertTrue(
                                        result.getResolvedException()
                                                instanceof ResourceNotFoundException))
                .andExpect(
                        result ->
                                assertEquals(
                                        "No entries for B0B1J1H6A7 were found",
                                        result.getResolvedException().getMessage()));
    }

    @Test
    void searchAccessionSpecificVersionRedirectToUnisave() throws Exception {
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(ACCESSION_RESOURCE, "A0A1J4H6S2")
                                        .param("version", "5")
                                        .header(ACCEPT, FF_MEDIA_TYPE_VALUE));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.SEE_OTHER.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, FF_MEDIA_TYPE_VALUE))
                .andExpect(
                        header().string(
                                        HttpHeaders.LOCATION,
                                        getRedirectToEntryVersionPath("A0A1J4H6S2", "5", "txt")));
    }

    @Test
    void searchAccessionSpecificVersionWithNonAllowedJsonFormat() throws Exception {
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(ACCESSION_RESOURCE, "A0A1J4H6S2")
                                        .param("version", "3")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "Expected one of [text/plain;format=fasta, text/plain;format=flatfile]")));
    }

    @Test
    void searchAccessionSpecificVersionWithNonAllowedTsvFormat() throws Exception {
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(ACCESSION_RESOURCE, "A0A1J4H6S2")
                                        .param("version", "5")
                                        .header(ACCEPT, TSV_MEDIA_TYPE_VALUE));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, TSV_MEDIA_TYPE_VALUE))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "Expected one of [text/plain;format=fasta, text/plain;format=flatfile]")));
    }

    private String getRedirectToEntryVersionPath(String accession, String version, String format) {
        return String.format(
                "/unisave/%s?from=%s&versions=%s&format=%s", accession, accession, version, format);
    }

    @Override
    protected RestTemplate getRestTemple() {
        return restTemplate;
    }

    @Override
    protected String getSearchAccession() {
        return ACCESSION_ID;
    }

    @Override
    protected String getRdfProlog() {
        return RdfPrologs.UNIPROT_PROLOG;
    }

    @Override
    protected String getIdRequestPathWithoutPathVariable() {
        return "/uniprotkb/";
    }

    static class UniprotKBGetIdParameterResolver extends AbstractGetIdParameterResolver {

        private static final String NON_EXISTENT_ACCESSION_ID = "Q12345";

        @Override
        public GetIdParameter validIdParameter() {
            return GetIdParameter.builder()
                    .id(ACCESSION_ID)
                    .resultMatcher(jsonPath("$.primaryAccession", is(ACCESSION_ID)))
                    .build();
        }

        @Override
        public GetIdParameter invalidIdParameter() {
            return GetIdParameter.builder()
                    .id("invalid")
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    contains(
                                            "The 'accession' value has invalid format. It should be a valid UniProtKB accession")))
                    .build();
        }

        @Override
        public GetIdParameter nonExistentIdParameter() {
            return GetIdParameter.builder()
                    .id(NON_EXISTENT_ACCESSION_ID)
                    .resultMatcher(jsonPath("$.url", not(is(emptyOrNullString()))))
                    .resultMatcher(jsonPath("$.messages.*", contains("Resource not found")))
                    .build();
        }

        @Override
        public GetIdParameter withFilterFieldsParameter() {
            return GetIdParameter.builder()
                    .id(ACCESSION_ID)
                    .fields("gene_primary,cc_function,cc_pathway")
                    .resultMatcher(jsonPath("$.primaryAccession", is(ACCESSION_ID)))
                    .resultMatcher(jsonPath("$.genes").exists())
                    .resultMatcher(jsonPath("$.comments[?(@.commentType=='FUNCTION')]").exists())
                    .resultMatcher(jsonPath("$.comments[?(@.commentType=='PATHWAY')]").exists())
                    // ensure other parts of the entry were not returned (using one example)
                    .resultMatcher(jsonPath("$.organism").doesNotExist())
                    .resultMatcher(
                            jsonPath("$.comments[?(@.commentType=='SIMILARITY')]").doesNotExist())
                    .build();
        }

        @Override
        public GetIdParameter withInvalidFilterParameter() {
            return GetIdParameter.builder()
                    .id(ACCESSION_ID)
                    .fields("invalid")
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    contains("Invalid fields parameter value 'invalid'")))
                    .build();
        }
    }

    static class UniprotKBGetIdContentTypeParamResolver
            extends AbstractGetIdContentTypeParamResolver {
        @Override
        public GetIdContentTypeParam idSuccessContentTypesParam() {
            return GetIdContentTypeParam.builder()
                    .id(ACCESSION_ID)
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(jsonPath("$.primaryAccession", is(ACCESSION_ID)))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.entryType",
                                                    is("UniProtKB reviewed (Swiss-Prot)")))
                                    .resultMatcher(jsonPath("$.uniProtkbId", is("PURL_THEEB")))
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
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "ID   PURL_THEEB              Reviewed;         761 AA.\n"
                                                                            + "AC   Q8DIA7;\n"
                                                                            + "DT   07-JUN-2005, integrated into UniProtKB/Swiss-Prot.\n"
                                                                            + "DT   01-MAR-2003, sequence version 1.\n"
                                                                            + "DT   11-DEC-2019, entry version 106.")))
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
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.GFF_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "##gff-version 3\n"
                                                                            + "##sequence-region Q8DIA7 1 761\n"
                                                                            + "Q8DIA7\tUniProtKB\tChain\t1\t761\t.\t.\t.\tID=PRO_0000100496;Note=Phosphoribosylformylglycinamidine synthase subunit PurL")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(content().string(containsString(ACCESSION_ID)))
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
                                                                    "Q8DIA7\tPURL_THEEB\treviewed\tPhosphoribosylformylglycinamidine synthase subunit PurL (FGAM synthase)")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                                    .resultMatcher(
                                            content().contentType(UniProtMediaType.XLS_MEDIA_TYPE))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.RDF_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "<?xml version='1.0' encoding='UTF-8'?>\n"
                                                                            + "<rdf:RDF xml:base=\"http://purl.uniprot.org/uniprot/\" xmlns=\"http://purl.uniprot.org/core/\" xmlns:ECO=\"http://purl.obolibrary.org/obo/ECO_\" xmlns:annotation=\"http://purl.uniprot.org/annotation/\" xmlns:citation=\"http://purl.uniprot.org/citations/\" xmlns:dcterms=\"http://purl.org/dc/terms/\" xmlns:disease=\"http://purl.uniprot.org/diseases/\" xmlns:enzyme=\"http://purl.uniprot.org/enzyme/\" xmlns:faldo=\"http://biohackathon.org/resource/faldo#\" xmlns:foaf=\"http://xmlns.com/foaf/0.1/\" xmlns:go=\"http://purl.obolibrary.org/obo/GO_\" xmlns:isoform=\"http://purl.uniprot.org/isoforms/\" xmlns:keyword=\"http://purl.uniprot.org/keywords/\" xmlns:location=\"http://purl.uniprot.org/locations/\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\" xmlns:position=\"http://purl.uniprot.org/position/\" xmlns:pubmed=\"http://purl.uniprot.org/pubmed/\" xmlns:range=\"http://purl.uniprot.org/range/\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" xmlns:skos=\"http://www.w3.org/2004/02/skos/core#\" xmlns:taxon=\"http://purl.uniprot.org/taxonomy/\" xmlns:tissue=\"http://purl.uniprot.org/tissues/\">\n"
                                                                            + "<owl:Ontology rdf:about=\"http://purl.uniprot.org/uniprot/\">\n"
                                                                            + "<owl:imports rdf:resource=\"http://purl.uniprot.org/core/\"/>\n"
                                                                            + "</owl:Ontology>\n"
                                                                            + "    <sample>text</sample>\n"
                                                                            + "    <anotherSample>text2</anotherSample>\n"
                                                                            + "    <someMore>text3</someMore>\n"
                                                                            + "</rdf:RDF>")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TURTLE_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            startsWith(
                                                                    TurtlePrologs.UNIPROT_PROLOG)))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "    <sample>text</sample>\n"
                                                                            + "    <anotherSample>text2</anotherSample>\n"
                                                                            + "    <someMore>text3</someMore>\n"
                                                                            + "</rdf:RDF>")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.N_TRIPLES_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            startsWith(
                                                                    NTriplesPrologs
                                                                            .N_TRIPLES_COMMON_PROLOG)))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "    <sample>text</sample>\n"
                                                                            + "    <anotherSample>text2</anotherSample>\n"
                                                                            + "    <someMore>text3</someMore>\n"
                                                                            + "</rdf:RDF>")))
                                    .build())
                    .build();
        }

        @Override
        public GetIdContentTypeParam idBadRequestContentTypesParam() {
            return GetIdContentTypeParam.builder()
                    .id("INVALID")
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(jsonPath("$.url", not(is(emptyOrNullString()))))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.messages.*",
                                                    contains(
                                                            "The 'accession' value has invalid format. It should be a valid UniProtKB accession")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_XML)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "<messages>The 'accession' value has invalid format. It should be a valid UniProtKB accession</messages>")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.FF_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            is(
                                                                    "Error messages\nThe 'accession' value has invalid format. It should be a valid UniProtKB accession")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.FASTA_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            is(
                                                                    "Error messages\nThe 'accession' value has invalid format. It should be a valid UniProtKB accession")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.GFF_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            is(
                                                                    "Error messages\nThe 'accession' value has invalid format. It should be a valid UniProtKB accession")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            is(
                                                                    "Error messages\nThe 'accession' value has invalid format. It should be a valid UniProtKB accession")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            is(
                                                                    "Error messages\nThe 'accession' value has invalid format. It should be a valid UniProtKB accession")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                                    .resultMatcher(
                                            content().contentType(UniProtMediaType.XLS_MEDIA_TYPE))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.RDF_MEDIA_TYPE)
                                    .resultMatcher(content().string(not(is(emptyOrNullString()))))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(TURTLE_MEDIA_TYPE)
                                    .resultMatcher(content().string(not(is(emptyOrNullString()))))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(N_TRIPLES_MEDIA_TYPE)
                                    .resultMatcher(content().string(not(is(emptyOrNullString()))))
                                    .build())
                    .build();
        }
    }
}
