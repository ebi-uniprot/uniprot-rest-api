package org.uniprot.api.uniprotkb.controller;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.uniprot.api.rest.output.UniProtMediaType.FASTA_MEDIA_TYPE_VALUE;
import static org.uniprot.api.uniprotkb.controller.UniProtKBController.UNIPROTKB_RESOURCE;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.jupiter.api.Tag;
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
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.rest.controller.AbstractGetByIdWithTypeExtensionControllerIT;
import org.uniprot.api.rest.controller.AbstractStreamControllerIT;
import org.uniprot.api.rest.controller.param.ContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdParameter;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetByIdParameterResolver;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetIdContentTypeParamResolver;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.converter.ConverterConstants;
import org.uniprot.api.uniprotkb.UniProtKBREST;
import org.uniprot.api.uniprotkb.common.repository.UniProtKBDataStoreTestConfig;
import org.uniprot.api.uniprotkb.common.repository.search.UniprotQueryRepository;
import org.uniprot.api.uniprotkb.common.repository.store.UniProtKBStoreClient;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.UniSaveClient;
import org.uniprot.core.uniprotkb.DeletedReason;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.uniprotkb.impl.UniProtKBEntryBuilder;
import org.uniprot.store.datastore.voldemort.uniprot.VoldemortInMemoryUniprotEntryStore;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.indexer.uniprot.inactiveentry.InactiveUniProtEntry;
import org.uniprot.store.indexer.uniprot.mockers.InactiveEntryMocker;
import org.uniprot.store.indexer.uniprot.mockers.UniProtEntryMocker;
import org.uniprot.store.indexer.uniprotkb.processor.InactiveEntryConverter;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.uniprot.UniProtDocument;
import org.uniprot.store.spark.indexer.uniprot.converter.UniProtEntryConverter;

/**
 * @author lgonzales
 */
@ContextConfiguration(classes = {UniProtKBDataStoreTestConfig.class, UniProtKBREST.class})
@ActiveProfiles(profiles = "offline")
@AutoConfigureWebClient
@WebMvcTest(UniProtKBController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
            UniProtKBByAccessionControllerIT.UniprotKBGetByIdParameterResolver.class,
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
                        UniProtMediaType.getFileExtension(MediaType.APPLICATION_JSON),
                        "P00000",
                        "P99999",
                        "MY_ID",
                        "MY_ID"),
                Arguments.of(
                        UniProtMediaType.getFileExtension(UniProtMediaType.FF_MEDIA_TYPE),
                        "P00000",
                        "P99999",
                        "MY_ID",
                        "MY_ID"),
                Arguments.of(
                        UniProtMediaType.getFileExtension(UniProtMediaType.GFF_MEDIA_TYPE),
                        "P00000",
                        "P99999",
                        "MY_ID",
                        "P99999"),
                Arguments.of(
                        UniProtMediaType.getFileExtension(UniProtMediaType.LIST_MEDIA_TYPE),
                        "P00000",
                        "P99999",
                        "MY_ID",
                        "P99999"),
                Arguments.of(
                        UniProtMediaType.getFileExtension(UniProtMediaType.TSV_MEDIA_TYPE),
                        "P00000",
                        "P99999",
                        "MY_ID",
                        "P99999"),
                Arguments.of(
                        UniProtMediaType.getFileExtension(MediaType.APPLICATION_XML),
                        "P00000",
                        "P99999",
                        "MY_ID",
                        "P99999"),
                Arguments.of(
                        UniProtMediaType.getFileExtension(UniProtMediaType.XLS_MEDIA_TYPE),
                        "P00000",
                        "P99999",
                        "MY_ID",
                        "BINARY"),
                Arguments.of(
                        UniProtMediaType.getFileExtension(UniProtMediaType.FASTA_MEDIA_TYPE),
                        "P00000",
                        "P99999",
                        "MY_ID",
                        "P99999"),
                Arguments.of(
                        UniProtMediaType.getFileExtension(UniProtMediaType.RDF_MEDIA_TYPE),
                        "P00000",
                        "P99999",
                        "MY_ID",
                        "P00000"),
                Arguments.of(
                        UniProtMediaType.getFileExtension(UniProtMediaType.TURTLE_MEDIA_TYPE),
                        "P00000",
                        "P99999",
                        "MY_ID",
                        "P00000"),
                Arguments.of(
                        UniProtMediaType.getFileExtension(UniProtMediaType.N_TRIPLES_MEDIA_TYPE),
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
        initUniProtKbDataStore();
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);
    }

    @Override
    protected String getIdRequestPath() {
        return ACCESSION_RESOURCE;
    }

    void initUniProtKbDataStore() {
        UniProtEntryConverter uniProtEntryConverter =
                new UniProtEntryConverter(new HashMap<>(), new HashMap<>());

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
                                MockMvcRequestBuilders.get(ACCESSION_RESOURCE, ACCESSION_ID)
                                        .param("fields", "invalid, organism_name, invalid2")
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
                                MockMvcRequestBuilders.get(ACCESSION_RESOURCE, "P21802-2")
                                        .param("fields", "accession,organism_name")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.primaryAccession", is("P21802-2")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.organism").exists())
                // ensure other parts of the entry were not returned (using one example)
                .andExpect(MockMvcResultMatchers.jsonPath("$.genes").doesNotExist());
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
                                MockMvcRequestBuilders.get(ACCESSION_RESOURCE, "P21802-1")
                                        .param("fields", "accession,organism_name")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        // then return the canonical isoform entry
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.primaryAccession", is("P21802-1")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.organism").exists())
                // ensure other parts of the entry were not returned (using one example)
                .andExpect(MockMvcResultMatchers.jsonPath("$.comments").doesNotExist());

        // when is an accession without isoforms (Alternative products comment)
        response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(ACCESSION_RESOURCE, "q8dia7-1")
                                        .param("fields", "accession,organism_name")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        // then return the canonical entry
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.primaryAccession", is("Q8DIA7")));
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
                                MockMvcRequestBuilders.get(ACCESSION_RESOURCE, "B4DFC2")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.SEE_OTHER.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.LOCATION, "/uniprotkb/P21802?from=B4DFC2"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.primaryAccession", is("B4DFC2")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.entryType", is("Inactive")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.extraAttributes.uniParcId", is("UPI000012CEBD")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.inactiveReason.inactiveReasonType", is("MERGED")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.extraAttributes.uniParcId", is("UPI000012CEBD")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.inactiveReason.mergeDemergeTo", contains("P21802")));
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
                InactiveUniProtEntry.from(
                        inactiveAcc,
                        "I8FBX0_MYCAB",
                        InactiveEntryMocker.MERGED,
                        "UPI0001661588",
                        activeAcc,
                        null);
        getStoreManager()
                .saveEntriesInSolr(DataStoreManager.StoreType.INACTIVE_UNIPROT, inactiveEntry);

        // REQUEST 1 ---------------------------
        // WHEN we fetch the inactive accession
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(
                                        ACCESSION_RESOURCE, inactiveAcc + "." + fileExtension));

        // THEN we expect a redirect 303 (200 for RDF)
        ResultActions resultActions = response.andDo(MockMvcResultHandlers.log());
        String redirectedURL = "/uniprotkb/" + activeAcc;
        if (!mediaType.equals(UniProtMediaType.DEFAULT_MEDIA_TYPE)) {
            redirectedURL += "." + fileExtension;
        }
        redirectedURL += "?from=" + inactiveAcc;

        if (Set.of(
                        UniProtMediaType.RDF_MEDIA_TYPE,
                        UniProtMediaType.TURTLE_MEDIA_TYPE,
                        UniProtMediaType.N_TRIPLES_MEDIA_TYPE)
                .contains(mediaType)) {
            resultActions
                    .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                    .andExpect(
                            MockMvcResultMatchers.header()
                                    .string(HttpHeaders.CONTENT_TYPE, mediaType.toString()));
        } else {
            // response REDIRECTS client
            resultActions
                    .andExpect(MockMvcResultMatchers.status().is(HttpStatus.SEE_OTHER.value()))
                    .andExpect(
                            MockMvcResultMatchers.header()
                                    .string(HttpHeaders.CONTENT_TYPE, mediaType.toString()))
                    .andExpect(
                            MockMvcResultMatchers.header()
                                    .string(HttpHeaders.LOCATION, redirectedURL));

            // REQUEST 2 ---------------------------
            // ... let's simulate a client (e.g., browser) redirect and go to the address directly
            response = getMockMvc().perform(MockMvcRequestBuilders.get(redirectedURL));

            // then we expect the active entry to contain the appropriate contents
            Matcher<String> matcher = containsString(expectResponseMatch);
            if (expectResponseMatch.equals("BINARY")) {
                matcher = is(not(emptyString()));
            }

            response.andDo(MockMvcResultHandlers.log())
                    .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                    .andExpect(
                            MockMvcResultMatchers.header()
                                    .string(HttpHeaders.CONTENT_TYPE, mediaType.toString()))
                    .andExpect(MockMvcResultMatchers.content().string(matcher));
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
                                MockMvcRequestBuilders.get(ACCESSION_RESOURCE, "Q00007")
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
                        MockMvcResultMatchers.content()
                                .contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.primaryAccession", is("Q00007")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.entryType", is("Inactive")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.extraAttributes.uniParcId", is("UPI000012CEBF")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.inactiveReason.inactiveReasonType", is("DEMERGED")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.inactiveReason.mergeDemergeTo", contains("P21802", "P63151")));
    }

    @Test
    void searchForDeletedInactiveEntriesReturnItselfWithoutDeletedReason() throws Exception {
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
                                MockMvcRequestBuilders.get(ACCESSION_RESOURCE, "I8FBX2")
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
                        MockMvcResultMatchers.content()
                                .contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.primaryAccession", is("I8FBX2")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.entryType", is("Inactive")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.extraAttributes.uniParcId", is("UPI000012CEBB")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.inactiveReason.inactiveReasonType", is("DELETED")))
                .andExpect(jsonPath("$.inactiveReason.deletedReason").doesNotExist());
    }

    @Test
    void searchForDeletedInactiveEntriesReturnItselfWithDeletedReason() throws Exception {
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
                                MockMvcRequestBuilders.get(ACCESSION_RESOURCE, "I8FBX1")
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
                        MockMvcResultMatchers.content()
                                .contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.primaryAccession", is("I8FBX1")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.entryType", is("Inactive")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.extraAttributes.uniParcId", is("UPI00000DCD3D")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.inactiveReason.inactiveReasonType", is("DELETED")))
                .andExpect(
                        jsonPath(
                                "$.inactiveReason.deletedReason",
                                is(DeletedReason.PROTEOME_REDUNDANCY.getDisplayName())));
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
                                MockMvcRequestBuilders.get(ACCESSION_RESOURCE, "P21802.20")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.SEE_OTHER.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.content()
                                .contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(
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
                                MockMvcRequestBuilders.get(ACCESSION_RESOURCE, "FGFR2_HUMAN")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.SEE_OTHER.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.content()
                                .contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(
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
                                MockMvcRequestBuilders.get(ACCESSION_RESOURCE, "FGFR2_DOG")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.content()
                                .contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.header().string(HttpHeaders.LOCATION, nullValue()));
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
                                MockMvcRequestBuilders.get(ACCESSION_RESOURCE, "FGFR2_HUMAN")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.SEE_OTHER.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.content()
                                .contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(
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
                                MockMvcRequestBuilders.get(ACCESSION_RESOURCE, "Q14301_FGFR2")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.SEE_OTHER.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.content()
                                .contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(
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
                                MockMvcRequestBuilders.get(ACCESSION_RESOURCE, "I8FBX2_YERPE")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.SEE_OTHER.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.content()
                                .contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(
                                        HttpHeaders.LOCATION,
                                        "/uniprotkb/I8FBX2?from=I8FBX2_YERPE"));
    }

    @Test
    void searchWithObsoleteIDRedirectToAccession() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        UniProtEntryConverter uniProtEntryConverter =
                new UniProtEntryConverter(new HashMap<>(), new HashMap<>());
        UniProtDocument doc = uniProtEntryConverter.convert(entry);
        doc.id.add("OBS_ID"); // first id is primary, all the others added are obsolete.
        getStoreManager().saveDocs(DataStoreManager.StoreType.UNIPROT, doc);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(ACCESSION_RESOURCE, "OBS_ID")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.SEE_OTHER.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.content()
                                .contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.LOCATION, "/uniprotkb/P21802?from=OBS_ID"));
    }

    @Test
    @Tag("TRM-29946")
    void searchWithDemergedObsoleteIDReturnsBadRequest() throws Exception {
        // given
        UniProtEntryConverter uniProtEntryConverter =
                new UniProtEntryConverter(new HashMap<>(), new HashMap<>());
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
        UniProtDocument doc1 = uniProtEntryConverter.convert(entry);
        doc1.id.add("YK29_YEAST"); // adding obsolete id.

        entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.TR);
        UniProtDocument doc2 = uniProtEntryConverter.convert(entry);
        doc2.id.add("YK29_YEAST"); // adding obsolete id.

        getStoreManager().saveDocs(DataStoreManager.StoreType.UNIPROT, doc1, doc2);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(ACCESSION_RESOURCE, "YK29_YEAST")
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
                                containsInAnyOrder(
                                        "Invalid request received. This protein ID 'YK29_YEAST' is now obsolete. Please refer to the accessions derived from this protein ID (F1Q0X3, P21802).")));
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
                                MockMvcRequestBuilders.get(ACCESSION_RESOURCE, "A0A1J4H6S2")
                                        .param("version", "last")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                UniProtMediaType.FF_MEDIA_TYPE_VALUE));
        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.SEE_OTHER.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(
                                        HttpHeaders.CONTENT_TYPE,
                                        UniProtMediaType.FF_MEDIA_TYPE_VALUE))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(
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
                                MockMvcRequestBuilders.get(ACCESSION_RESOURCE, "B0B1J1H6A7")
                                        .param("version", "last")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                UniProtMediaType.FF_MEDIA_TYPE_VALUE));
        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(
                        result ->
                                assertTrue(
                                        result.getResolvedException()
                                                instanceof ResourceNotFoundException))
                .andExpect(
                        result ->
                                Assert.assertEquals(
                                        "No entries for B0B1J1H6A7 were found",
                                        result.getResolvedException().getMessage()));
    }

    @Test
    void searchAccessionSpecificVersionRedirectToUnisave() throws Exception {
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(ACCESSION_RESOURCE, "A0A1J4H6S2")
                                        .param("version", "5")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                UniProtMediaType.FF_MEDIA_TYPE_VALUE));
        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.SEE_OTHER.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(
                                        HttpHeaders.CONTENT_TYPE,
                                        UniProtMediaType.FF_MEDIA_TYPE_VALUE))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(
                                        HttpHeaders.LOCATION,
                                        getRedirectToEntryVersionPath("A0A1J4H6S2", "5", "txt")));
    }

    @Test
    void searchAccessionSpecificVersionWithNonAllowedJsonFormat() throws Exception {
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(ACCESSION_RESOURCE, "A0A1J4H6S2")
                                        .param("version", "3")
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
                        MockMvcResultMatchers.content()
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
                                MockMvcRequestBuilders.get(ACCESSION_RESOURCE, "A0A1J4H6S2")
                                        .param("version", "5")
                                        .header(
                                                HttpHeaders.ACCEPT,
                                                UniProtMediaType.TSV_MEDIA_TYPE_VALUE));
        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(
                                        HttpHeaders.CONTENT_TYPE,
                                        UniProtMediaType.TSV_MEDIA_TYPE_VALUE))
                .andExpect(
                        MockMvcResultMatchers.content()
                                .string(
                                        containsString(
                                                "Expected one of [text/plain;format=fasta, text/plain;format=flatfile]")));
    }

    @Test
    void testGetEntryForAGivenSequenceRange() throws Exception {
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(ACCESSION_RESOURCE, "Q8DIA7[10-20]")
                                        .header(ACCEPT, FASTA_MEDIA_TYPE_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, FASTA_MEDIA_TYPE_VALUE))
                .andExpect(content().string(is(">sp|Q8DIA7|10-20\n" + "AEITAEGLKPQ\n")));
    }

    @Test
    void testGetEntryForAGivenSequenceRangeWithFileExtension() throws Exception {
        // when
        ResultActions response = getMockMvc().perform(get(ACCESSION_RESOURCE, "Q8DIA7[1-5].fasta"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, FASTA_MEDIA_TYPE_VALUE))
                .andExpect(content().string(is(">sp|Q8DIA7|1-5\n" + "MSQTP\n")));
    }

    @Test
    void testGetEntryBeyondRangeReturnsEmptySequence() throws Exception {
        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(ACCESSION_RESOURCE, "Q8DIA7[9999-999999]")
                                        .header(ACCEPT, FASTA_MEDIA_TYPE_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, FASTA_MEDIA_TYPE_VALUE))
                .andExpect(content().string(is(">sp|Q8DIA7|9999-999999\n\n")));
    }

    @Test
    void testGetIsoformEntrySequenceRange() throws Exception {
        // given
        UniProtKBEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_ISOFORM);
        getStoreManager().save(DataStoreManager.StoreType.UNIPROT, entry);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(ACCESSION_RESOURCE, "P21802-2[10-20]")
                                        .header(ACCEPT, FASTA_MEDIA_TYPE_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, FASTA_MEDIA_TYPE_VALUE))
                .andExpect(content().string(is(">sp|P21802-2|10-20\nLVVVTMATLSL\n")));
    }

    @ParameterizedTest
    @MethodSource("provideInvalidAccession")
    void testGetEntryForGivenInvalidRanges(String accession, String format, ResultMatcher matcher)
            throws Exception {
        // when
        ResultActions response =
                getMockMvc().perform(get(ACCESSION_RESOURCE, accession).header(ACCEPT, format));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, format))
                .andExpect(matcher);
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
    protected String getIdRequestPathWithoutPathVariable() {
        return "/uniprotkb/";
    }

    static class UniprotKBGetByIdParameterResolver extends AbstractGetByIdParameterResolver {

        private static final String NON_EXISTENT_ACCESSION_ID = "Q12345";

        @Override
        public GetIdParameter validIdParameter() {
            return GetIdParameter.builder()
                    .id(ACCESSION_ID)
                    .resultMatcher(
                            MockMvcResultMatchers.jsonPath("$.primaryAccession", is(ACCESSION_ID)))
                    .build();
        }

        @Override
        public GetIdParameter invalidIdParameter() {
            return GetIdParameter.builder()
                    .id("invalid")
                    .resultMatcher(
                            MockMvcResultMatchers.jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(
                            MockMvcResultMatchers.jsonPath(
                                    "$.messages.*",
                                    contains(
                                            "The 'accession' value has invalid format. It should be a valid UniProtKB accession")))
                    .build();
        }

        @Override
        public GetIdParameter nonExistentIdParameter() {
            return GetIdParameter.builder()
                    .id(NON_EXISTENT_ACCESSION_ID)
                    .resultMatcher(
                            MockMvcResultMatchers.jsonPath("$.url", not(is(emptyOrNullString()))))
                    .resultMatcher(
                            MockMvcResultMatchers.jsonPath(
                                    "$.messages.*", contains("Resource not found")))
                    .build();
        }

        @Override
        public GetIdParameter withFilterFieldsParameter() {
            return GetIdParameter.builder()
                    .id(ACCESSION_ID)
                    .fields("gene_primary,cc_function,cc_pathway")
                    .resultMatcher(
                            MockMvcResultMatchers.jsonPath("$.primaryAccession", is(ACCESSION_ID)))
                    .resultMatcher(MockMvcResultMatchers.jsonPath("$.genes").exists())
                    .resultMatcher(
                            MockMvcResultMatchers.jsonPath(
                                            "$.comments[?(@.commentType=='FUNCTION')]")
                                    .exists())
                    .resultMatcher(
                            MockMvcResultMatchers.jsonPath(
                                            "$.comments[?(@.commentType=='PATHWAY')]")
                                    .exists())
                    // ensure other parts of the entry were not returned (using one example)
                    .resultMatcher(MockMvcResultMatchers.jsonPath("$.organism").doesNotExist())
                    .resultMatcher(
                            MockMvcResultMatchers.jsonPath(
                                            "$.comments[?(@.commentType=='SIMILARITY')]")
                                    .doesNotExist())
                    .build();
        }

        @Override
        public GetIdParameter withInvalidFilterParameter() {
            return GetIdParameter.builder()
                    .id(ACCESSION_ID)
                    .fields("invalid")
                    .resultMatcher(
                            MockMvcResultMatchers.jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(
                            MockMvcResultMatchers.jsonPath(
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
                                    .resultMatcher(
                                            MockMvcResultMatchers.jsonPath(
                                                    "$.primaryAccession", is(ACCESSION_ID)))
                                    .resultMatcher(
                                            MockMvcResultMatchers.jsonPath(
                                                    "$.entryType",
                                                    is("UniProtKB reviewed (Swiss-Prot)")))
                                    .resultMatcher(
                                            MockMvcResultMatchers.jsonPath(
                                                    "$.uniProtkbId", is("PURL_THEEB")))
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
                                            MockMvcResultMatchers.content()
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
                                            MockMvcResultMatchers.content()
                                                    .string(
                                                            containsString(
                                                                    "##gff-version 3\n"
                                                                            + "Q8DIA7\tUniProtKB\tChain\t1\t761\t.\t.\t.\tID=PRO_0000100496;Note=Phosphoribosylformylglycinamidine synthase subunit PurL")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(
                                            MockMvcResultMatchers.content()
                                                    .string(containsString(ACCESSION_ID)))
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
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                                    .resultMatcher(
                                            MockMvcResultMatchers.content()
                                                    .contentType(UniProtMediaType.XLS_MEDIA_TYPE))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.RDF_MEDIA_TYPE)
                                    .resultMatcher(
                                            MockMvcResultMatchers.content()
                                                    .string(
                                                            equalTo(
                                                                    AbstractStreamControllerIT
                                                                            .SAMPLE_RDF)))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TURTLE_MEDIA_TYPE)
                                    .resultMatcher(
                                            MockMvcResultMatchers.content()
                                                    .string(
                                                            equalTo(
                                                                    AbstractStreamControllerIT
                                                                            .SAMPLE_TTL)))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.N_TRIPLES_MEDIA_TYPE)
                                    .resultMatcher(
                                            MockMvcResultMatchers.content()
                                                    .string(
                                                            equalTo(
                                                                    AbstractStreamControllerIT
                                                                            .SAMPLE_N_TRIPLES)))
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
                                    .resultMatcher(
                                            MockMvcResultMatchers.jsonPath(
                                                    "$.url", not(is(emptyOrNullString()))))
                                    .resultMatcher(
                                            MockMvcResultMatchers.jsonPath(
                                                    "$.messages.*",
                                                    contains(
                                                            "The 'accession' value has invalid format. It should be a valid UniProtKB accession")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_XML)
                                    .resultMatcher(
                                            MockMvcResultMatchers.content()
                                                    .string(
                                                            containsString(
                                                                    "<messages>The 'accession' value has invalid format. It should be a valid UniProtKB accession</messages>")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.FF_MEDIA_TYPE)
                                    .resultMatcher(
                                            MockMvcResultMatchers.content()
                                                    .string(
                                                            is(
                                                                    "Error messages\nThe 'accession' value has invalid format. It should be a valid UniProtKB accession")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.FASTA_MEDIA_TYPE)
                                    .resultMatcher(
                                            MockMvcResultMatchers.content()
                                                    .string(
                                                            is(
                                                                    "Error messages\nThe 'accession' value has invalid format. It should be a valid UniProtKB accession")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.GFF_MEDIA_TYPE)
                                    .resultMatcher(
                                            MockMvcResultMatchers.content()
                                                    .string(
                                                            is(
                                                                    "Error messages\nThe 'accession' value has invalid format. It should be a valid UniProtKB accession")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(
                                            MockMvcResultMatchers.content()
                                                    .string(
                                                            is(
                                                                    "Error messages\nThe 'accession' value has invalid format. It should be a valid UniProtKB accession")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(
                                            MockMvcResultMatchers.content()
                                                    .string(
                                                            is(
                                                                    "Error messages\nThe 'accession' value has invalid format. It should be a valid UniProtKB accession")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                                    .resultMatcher(
                                            MockMvcResultMatchers.content()
                                                    .contentType(UniProtMediaType.XLS_MEDIA_TYPE))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.RDF_MEDIA_TYPE)
                                    .resultMatcher(
                                            MockMvcResultMatchers.content()
                                                    .string(not(is(emptyOrNullString()))))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TURTLE_MEDIA_TYPE)
                                    .resultMatcher(
                                            MockMvcResultMatchers.content()
                                                    .string(not(is(emptyOrNullString()))))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.N_TRIPLES_MEDIA_TYPE)
                                    .resultMatcher(
                                            MockMvcResultMatchers.content()
                                                    .string(not(is(emptyOrNullString()))))
                                    .build())
                    .build();
        }
    }

    private static Stream<Arguments> provideInvalidAccession() {
        return Stream.of(
                Arguments.of(
                        "P12345[10-20]",
                        APPLICATION_JSON_VALUE,
                        jsonPath(
                                "$.messages.*",
                                contains(
                                        "Invalid request received. Sequence range is only supported for type text/plain;format=fasta"))),
                Arguments.of(
                        "P12345[20-10]",
                        FASTA_MEDIA_TYPE_VALUE,
                        content()
                                .string(
                                        is(
                                                "Error messages\n"
                                                        + "Invalid request received. Invalid sequence range [20-10]"))),
                Arguments.of(
                        "P12345[0-10]",
                        FASTA_MEDIA_TYPE_VALUE,
                        content()
                                .string(
                                        is(
                                                "Error messages\n"
                                                        + "Invalid request received. Invalid sequence range [0-10]"))),
                Arguments.of(
                        "P12345[1-5147483647]",
                        FASTA_MEDIA_TYPE_VALUE,
                        content()
                                .string(
                                        is(
                                                "Error messages\n"
                                                        + "Invalid request received. Invalid sequence range [1-5147483647]"))),
                Arguments.of(
                        "P12345.3[1-10]",
                        FASTA_MEDIA_TYPE_VALUE,
                        content()
                                .string(
                                        is(
                                                "Error messages\nThe 'accession' value has invalid format. It should be a valid UniProtKB accession"))),
                Arguments.of(
                        "P12345[1-10].3",
                        FASTA_MEDIA_TYPE_VALUE,
                        content()
                                .string(
                                        is(
                                                "Error messages\nThe 'accession' value has invalid format. It should be a valid UniProtKB accession"))));
    }
}
