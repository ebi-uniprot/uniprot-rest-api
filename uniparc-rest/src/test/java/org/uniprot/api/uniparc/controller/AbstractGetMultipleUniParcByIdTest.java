package org.uniprot.api.uniparc.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.uniprot.api.rest.output.UniProtMediaType.LIST_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.TSV_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.XLS_MEDIA_TYPE_VALUE;

import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.uniparc.repository.UniParcQueryRepository;
import org.uniprot.api.uniparc.repository.store.UniParcStoreClient;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.util.Utils;
import org.uniprot.core.xml.jaxb.uniparc.Entry;
import org.uniprot.core.xml.uniparc.UniParcEntryConverter;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.datastore.voldemort.uniparc.VoldemortInMemoryUniParcEntryStore;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.indexer.uniparc.UniParcDocumentConverter;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.search.SolrCollection;

/**
 * @author sahmad
 * @created 13/08/2020
 */
@Slf4j
abstract class AbstractGetMultipleUniParcByIdTest {
    @RegisterExtension static DataStoreManager storeManager = new DataStoreManager();

    @Autowired private UniProtStoreClient<UniParcEntry> storeClient;

    @Autowired protected MockMvc mockMvc;

    private static final String UPI_PREF = "UPI0000083C";
    protected static String ACCESSION = "P12301";
    protected static String UNIPARC_ID = "UPI0000083C01";

    @Autowired private UniParcQueryRepository repository;

    protected abstract String getGetByIdEndpoint();

    protected abstract String getSearchValue();

    @BeforeAll
    void initDataStore() {
        storeManager.addSolrClient(DataStoreManager.StoreType.UNIPARC, SolrCollection.uniparc);
        ReflectionTestUtils.setField(
                repository,
                "solrClient",
                storeManager.getSolrClient(DataStoreManager.StoreType.UNIPARC));
        storeClient =
                new UniParcStoreClient(
                        VoldemortInMemoryUniParcEntryStore.getInstance("avro-uniparc"));
        storeManager.addStore(DataStoreManager.StoreType.UNIPARC, storeClient);

        storeManager.addDocConverter(
                DataStoreManager.StoreType.UNIPARC,
                new UniParcDocumentConverter(
                        TaxonomyRepoMocker.getTaxonomyRepo(), new HashMap<>()));

        // create 5 entries
        IntStream.rangeClosed(1, 5).forEach(this::saveEntry);
    }

    @AfterAll
    void cleanUp() {
        storeManager.cleanSolr(DataStoreManager.StoreType.UNIPARC);
        storeManager.close();
    }

    @ParameterizedTest(name = "[{index}] try to get by id with content-type {0}")
    @ValueSource(strings = {MediaType.APPLICATION_PDF_VALUE, UniProtMediaType.OBO_MEDIA_TYPE_VALUE})
    void testGetByIdWithUnsupportedContentTypes(String contentType) throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getGetByIdEndpoint(), getSearchValue())
                                .header(ACCEPT, contentType));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        jsonPath(
                                "$.messages[0]",
                                containsString(
                                        "Invalid request received. Requested media type/format not accepted:")));
    }

    @ParameterizedTest(name = "[{index}] get by id with content-type {0}")
    @ValueSource(
            strings = {
                "",
                MediaType.APPLICATION_XML_VALUE,
                MediaType.APPLICATION_JSON_VALUE,
                UniProtMediaType.FASTA_MEDIA_TYPE_VALUE,
                TSV_MEDIA_TYPE_VALUE,
                XLS_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE
            })
    void testGetByIdSupportedContentTypes(String contentType) throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getGetByIdEndpoint(), getSearchValue())
                                .header(ACCEPT, contentType));

        if (Utils.nullOrEmpty(contentType)) {
            contentType = MediaType.APPLICATION_JSON_VALUE;
        }
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, contentType));
    }

    @Test
    void testGetIdWithInvalidReturnedFields() throws Exception {
        String searchVal = getSearchValue();
        // when
        ResultActions response =
                mockMvc.perform(
                        get(getGetByIdEndpoint(), searchVal)
                                .param("fields", "randomField")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.messages", notNullValue()))
                .andExpect(jsonPath("$.messages", iterableWithSize(1)))
                .andExpect(
                        jsonPath(
                                "$.messages[0]",
                                containsString("Invalid fields parameter value ")));
    }

    @ParameterizedTest(name = "[{index}] return for fieldName {0} and paths: {1}")
    @MethodSource("getAllReturnedFields")
    void testGetByIdWithAllAvailableReturnedFields(String name, List<String> paths)
            throws Exception {
        String searchVal = getSearchValue();
        // when
        ResultActions response =
                mockMvc.perform(
                        get(getGetByIdEndpoint(), searchVal)
                                .param("fields", name)
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        ResultActions resultActions =
                response.andDo(log())
                        .andExpect(status().is(HttpStatus.OK.value()))
                        .andExpect(
                                header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE));

        for (String path : paths) {
            String returnFieldValidatePath =
                    getGetByIdEndpoint().contains("accession")
                            ? "$." + path
                            : "$.results[*]." + path;
            log.info("ReturnField:" + name + " Validation Path: " + returnFieldValidatePath);
            resultActions.andExpect(jsonPath(returnFieldValidatePath).hasJsonPath());
        }
    }

    private void saveEntry(int i) {
        UniParcEntry entry = UniParcControllerITUtils.createEntry(i, UPI_PREF);
        // append two more cross ref
        UniParcEntry updatedEntry = UniParcControllerITUtils.appendMoreXRefs(entry, i);
        UniParcEntryConverter converter = new UniParcEntryConverter();
        Entry xmlEntry = converter.toXml(updatedEntry);
        storeManager.saveEntriesInSolr(DataStoreManager.StoreType.UNIPARC, xmlEntry);
        storeManager.saveToStore(DataStoreManager.StoreType.UNIPARC, updatedEntry);
    }

    protected static Stream<Arguments> getAllReturnedFields() {
        return ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.UNIPARC)
                .getReturnFields().stream()
                .map(returnField -> Arguments.of(returnField.getName(), returnField.getPaths()));
    }

    protected String extractCursor(ResultActions response) {
        String linkHeader = response.andReturn().getResponse().getHeader(HttpHeaders.LINK);
        assertThat(linkHeader, notNullValue());
        String cursor = linkHeader.split("\\?")[1].split("&")[0].split("=")[1];
        assertThat(cursor, notNullValue());
        return cursor;
    }
}
