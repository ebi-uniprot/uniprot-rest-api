package org.uniprot.api.uniparc.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.uniparc.UniParcRestApplication;
import org.uniprot.api.uniparc.repository.UniParcQueryRepository;
import org.uniprot.api.uniparc.repository.store.UniParcStoreClient;
import org.uniprot.core.Property;
import org.uniprot.core.uniparc.UniParcCrossReference;
import org.uniprot.core.uniparc.UniParcDatabase;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.uniparc.impl.UniParcCrossReferenceBuilder;
import org.uniprot.core.uniparc.impl.UniParcEntryBuilder;
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

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @author sahmad
 * @since 2020-08-11
 */
@Slf4j
@ContextConfiguration(classes = {UniParcDataStoreTestConfig.class, UniParcRestApplication.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(UniParcController.class)
@AutoConfigureWebClient
@ExtendWith(value = {SpringExtension.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UniParcGetByAccessionIT {
    @RegisterExtension static DataStoreManager storeManager = new DataStoreManager();

    private static final String getByAccessionPath = "/uniparc/accession/{accession}";

    @Autowired private UniProtStoreClient<UniParcEntry> storeClient;

    @Autowired private MockMvc mockMvc;

    private static final String UPI_PREF = "UPI0000083A";

    @Autowired private UniParcQueryRepository repository;

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
                new UniParcDocumentConverter(TaxonomyRepoMocker.getTaxonomyRepo()));

        // create 5 entries
        IntStream.rangeClosed(1, 5).forEach(this::saveEntry);
    }

    private void saveEntry(int i) {
        UniParcEntry entry = UniParcControllerITUtils.createEntry(i, UPI_PREF);
        // append two more cross ref
        UniParcEntry updatedEntry = appendTwoMoreXRefs(entry, i);
        UniParcEntryConverter converter = new UniParcEntryConverter();
        Entry xmlEntry = converter.toXml(updatedEntry);
        storeManager.saveEntriesInSolr(DataStoreManager.StoreType.UNIPARC, xmlEntry);
        storeManager.saveToStore(DataStoreManager.StoreType.UNIPARC, updatedEntry);
    }

    @Test
    void getByAccessionSuccess() throws Exception {
        // when
        String accession = "P12301";
        ResultActions response =
                mockMvc.perform(MockMvcRequestBuilders.get(getByAccessionPath, accession));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results", notNullValue()))
                .andExpect(jsonPath("$.results", iterableWithSize(1)))
                .andExpect(jsonPath("$.results[0].uniParcId", equalTo("UPI0000083A01")))
                .andExpect(jsonPath("$.results[0].uniParcCrossReferences", iterableWithSize(4)))
                .andExpect(
                        jsonPath("$.results[0].uniParcCrossReferences[*].id", hasItem(accession)))
                .andExpect(jsonPath("$.results[0].uniParcCrossReferences[*].id", notNullValue()))
                .andExpect(
                        jsonPath("$.results[0].uniParcCrossReferences[*].version", notNullValue()))
                .andExpect(
                        jsonPath("$.results[0].uniParcCrossReferences[*].versionI", notNullValue()))
                .andExpect(
                        jsonPath("$.results[0].uniParcCrossReferences[*].active", notNullValue()))
                .andExpect(
                        jsonPath("$.results[0].uniParcCrossReferences[*].created", notNullValue()))
                .andExpect(
                        jsonPath(
                                "$.results[0].uniParcCrossReferences[*].lastUpdated",
                                notNullValue()))
                .andExpect(
                        jsonPath("$.results[0].uniParcCrossReferences[*].database", notNullValue()))
                .andExpect(
                        jsonPath(
                                "$.results[0].uniParcCrossReferences[*].properties",
                                notNullValue()))
                .andExpect(
                        jsonPath(
                                "$.results[0].uniParcCrossReferences[0].properties",
                                iterableWithSize(4)))
                .andExpect(
                        jsonPath(
                                "$.results[0].uniParcCrossReferences[*].properties[*].key",
                                notNullValue()))
                .andExpect(
                        jsonPath(
                                "$.results[0].uniParcCrossReferences[*].properties[*].value",
                                notNullValue()))
                .andExpect(jsonPath("$.results[0].sequence", notNullValue()))
                .andExpect(jsonPath("$.results[0].sequence.value", notNullValue()))
                .andExpect(jsonPath("$.results[0].sequence.length", notNullValue()))
                .andExpect(jsonPath("$.results[0].sequence.molWeight", notNullValue()))
                .andExpect(jsonPath("$.results[0].sequence.crc64", notNullValue()))
                .andExpect(jsonPath("$.results[0].sequence.md5", notNullValue()))
                .andExpect(jsonPath("$.results[0].sequenceFeatures", iterableWithSize(13)))
                .andExpect(jsonPath("$.results[0].sequenceFeatures[*].database", notNullValue()))
                .andExpect(jsonPath("$.results[0].sequenceFeatures[*].databaseId", notNullValue()))
                .andExpect(jsonPath("$.results[0].sequenceFeatures[*].locations", notNullValue()))
                .andExpect(
                        jsonPath("$.results[0].sequenceFeatures[0].locations", iterableWithSize(2)))
                .andExpect(
                        jsonPath(
                                "$.results[0].sequenceFeatures[*].locations[*].start",
                                notNullValue()))
                .andExpect(
                        jsonPath(
                                "$.results[0].sequenceFeatures[*].locations[*].end",
                                notNullValue()))
                .andExpect(
                        jsonPath("$.results[0].sequenceFeatures[*].interproGroup", notNullValue()))
                .andExpect(
                        jsonPath(
                                "$.results[0].sequenceFeatures[*].interproGroup.id",
                                notNullValue()))
                .andExpect(
                        jsonPath(
                                "$.results[0].sequenceFeatures[*].interproGroup.name",
                                notNullValue()))
                .andExpect(jsonPath("$.results[0].taxonomies", iterableWithSize(2)))
                .andExpect(jsonPath("$.results[0].taxonomies[*].scientificName", notNullValue()))
                .andExpect(jsonPath("$.results[0].taxonomies[*].taxonId", notNullValue()));
    }

    @Test
    void getByAccessionWithDBFilterSuccess() throws Exception {
        // when
        String accession = "P12301";
        String dbTypes = "UniProtKB/TrEMBL,embl";
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getByAccessionPath, accession)
                                .param("dbTypes", dbTypes));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results", notNullValue()))
                .andExpect(jsonPath("$.results", iterableWithSize(1)))
                .andExpect(jsonPath("$.results[0].uniParcId", equalTo("UPI0000083A01")))
                .andExpect(jsonPath("$.results[0].uniParcCrossReferences", iterableWithSize(2)))
                .andExpect(
                        jsonPath("$.results[0].uniParcCrossReferences[*].id", hasItem(accession)))
                .andExpect(jsonPath("$.results[0].uniParcCrossReferences[*].id", notNullValue()))
                .andExpect(
                        jsonPath(
                                "$.results[0].uniParcCrossReferences[*].database",
                                containsInAnyOrder("UniProtKB/TrEMBL", "EMBL")))
                .andExpect(jsonPath("$.results[0].sequence", notNullValue()))
                .andExpect(jsonPath("$.results[0].sequenceFeatures", iterableWithSize(13)))
                .andExpect(jsonPath("$.results[0].taxonomies", iterableWithSize(2)));
    }

    @Test
    void getByAccessionWithDBIdsFilterSuccess() throws Exception {
        // when
        String accession = "P12301";
        String dbIds = "unimes1,P10001,randomId";
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getByAccessionPath, accession)
                                .param("dbIds", dbIds));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results", notNullValue()))
                .andExpect(jsonPath("$.results", iterableWithSize(1)))
                .andExpect(jsonPath("$.results[0].uniParcId", equalTo("UPI0000083A01")))
                .andExpect(jsonPath("$.results[0].uniParcCrossReferences", iterableWithSize(2)))
                .andExpect(jsonPath("$.results[0].uniParcCrossReferences[*].id", notNullValue()))
                .andExpect(
                        jsonPath(
                                "$.results[0].uniParcCrossReferences[*].id",
                                containsInAnyOrder("unimes1", "P10001")))
                .andExpect(
                        jsonPath("$.results[0].uniParcCrossReferences[*].database", notNullValue()))
                .andExpect(jsonPath("$.results[0].sequence", notNullValue()))
                .andExpect(jsonPath("$.results[0].sequenceFeatures", iterableWithSize(13)))
                .andExpect(jsonPath("$.results[0].taxonomies", iterableWithSize(2)));
    }

    @Test
    void getByAccessionWithTaxonomyIdsFilterSuccess() throws Exception {
        // when
        String accession = "P12301";
        String taxonIds = "9606,radomTaxonId";
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getByAccessionPath, accession)
                                .param("taxonIds", taxonIds));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results", notNullValue()))
                .andExpect(jsonPath("$.results", iterableWithSize(1)))
                .andExpect(jsonPath("$.results[0].uniParcId", equalTo("UPI0000083A01")))
                .andExpect(jsonPath("$.results[0].uniParcCrossReferences", iterableWithSize(4)))
                .andExpect(jsonPath("$.results[0].uniParcCrossReferences[*].id", notNullValue()))
                .andExpect(jsonPath("$.results[0].uniParcCrossReferences[*].id", hasItem("P12301")))
                .andExpect(
                        jsonPath("$.results[0].uniParcCrossReferences[*].database", notNullValue()))
                .andExpect(jsonPath("$.results[0].sequence", notNullValue()))
                .andExpect(jsonPath("$.results[0].sequenceFeatures", iterableWithSize(13)))
                .andExpect(jsonPath("$.results[0].taxonomies", iterableWithSize(1)))
                .andExpect(jsonPath("$.results[0].taxonomies[0].taxonId", is(9606)));
    }

    @Test
    void getByAccessionWithActiveCrossRefFilterSuccess() throws Exception {
        // when
        String accession = "P12301";
        String active = "true";
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getByAccessionPath, accession)
                                .param("active", active));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results", notNullValue()))
                .andExpect(jsonPath("$.results", iterableWithSize(1)))
                .andExpect(jsonPath("$.results[0].uniParcId", equalTo("UPI0000083A01")))
                .andExpect(jsonPath("$.results[0].uniParcCrossReferences", iterableWithSize(3)))
                .andExpect(jsonPath("$.results[0].uniParcCrossReferences[*].id", notNullValue()))
                .andExpect(jsonPath("$.results[0].uniParcCrossReferences[*].id", hasItem("P12301")))
                .andExpect(
                        jsonPath("$.results[0].uniParcCrossReferences[*].database", notNullValue()))
                .andExpect(
                        jsonPath(
                                "$.results[0].uniParcCrossReferences[*].active",
                                contains(true, true, true)))
                .andExpect(jsonPath("$.results[0].sequence", notNullValue()))
                .andExpect(jsonPath("$.results[0].sequenceFeatures", iterableWithSize(13)))
                .andExpect(jsonPath("$.results[0].taxonomies", iterableWithSize(2)));
    }

    @Test
    void getByAccessionWithInActiveCrossRefFilterSuccess() throws Exception {
        // when
        String accession = "P12301";
        String active = "false";
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getByAccessionPath, accession)
                                .param("active", active));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results", notNullValue()))
                .andExpect(jsonPath("$.results", iterableWithSize(1)))
                .andExpect(jsonPath("$.results[0].uniParcId", equalTo("UPI0000083A01")))
                .andExpect(jsonPath("$.results[0].uniParcCrossReferences", iterableWithSize(1)))
                .andExpect(jsonPath("$.results[0].uniParcCrossReferences[*].id", notNullValue()))
                .andExpect(
                        jsonPath("$.results[0].uniParcCrossReferences[*].database", notNullValue()))
                .andExpect(
                        jsonPath("$.results[0].uniParcCrossReferences[*].active", contains(false)))
                .andExpect(jsonPath("$.results[0].sequence", notNullValue()))
                .andExpect(jsonPath("$.results[0].sequenceFeatures", iterableWithSize(13)))
                .andExpect(jsonPath("$.results[0].taxonomies", iterableWithSize(2)));
    }

    @ParameterizedTest(name = "[{index}] return for fieldName {0} and paths: {1}")
    @MethodSource("getAllReturnedFields")
    void searchCanSearchWithAllAvailableReturnedFields(String name, List<String> paths)
            throws Exception {
        String accession = "P12301";
        // when
        ResultActions response =
                mockMvc.perform(
                        get(getByAccessionPath, accession)
                                .param("fields", name)
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        ResultActions resultActions =
                response.andDo(print())
                        .andExpect(status().is(HttpStatus.OK.value()))
                        .andExpect(
                                header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                        .andExpect(jsonPath("$.results.size()", greaterThan(0)));

        for (String path : paths) {
            String returnFieldValidatePath = "$.results[*]." + path;
            log.info("ReturnField:" + name + " Validation Path: " + returnFieldValidatePath);
            resultActions.andExpect(jsonPath(returnFieldValidatePath).hasJsonPath());
        }
    }

    @ParameterizedTest(name = "[{index}] get by accession with content-type {0}")
    @ValueSource(
            strings = {
                "",
                MediaType.APPLICATION_XML_VALUE,
                MediaType.APPLICATION_JSON_VALUE,
                UniProtMediaType.FASTA_MEDIA_TYPE_VALUE
            })
    void testGetBySupportedContentTypes(String contentType) throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getByAccessionPath, "P12301")
                                .header(ACCEPT, contentType));

        if (Utils.nullOrEmpty(contentType)) {
            contentType = MediaType.APPLICATION_JSON_VALUE;
        }
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, contentType));
    }

    @ParameterizedTest(name = "[{index}] try to get by accession with content-type {0}")
    @ValueSource(
            strings = {MediaType.APPLICATION_PDF_VALUE, UniProtMediaType.LIST_MEDIA_TYPE_VALUE})
    void testGetByAccessionWithUnsupportedContentTypes(String contentType) throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getByAccessionPath, "P12301")
                                .header(ACCEPT, contentType));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        jsonPath(
                                "$.messages[0]",
                                containsString(
                                        "Invalid request received. Requested media type/format not accepted:")));
    }

    private UniParcEntry appendTwoMoreXRefs(UniParcEntry entry, int i) {
        UniParcCrossReference xref1 =
                new UniParcCrossReferenceBuilder()
                        .versionI(1)
                        .database(UniParcDatabase.EMBL)
                        .id("embl" + i)
                        .version(7)
                        .active(true)
                        .created(LocalDate.of(2017, 2, 12))
                        .lastUpdated(LocalDate.of(2017, 4, 23))
                        .propertiesAdd(
                                new Property(
                                        UniParcCrossReference.PROPERTY_PROTEIN_NAME,
                                        "proteinName" + i))
                        .build();

        UniParcCrossReference xref2 =
                new UniParcCrossReferenceBuilder()
                        .versionI(1)
                        .database(UniParcDatabase.UNIMES)
                        .id("unimes" + i)
                        .version(7)
                        .active(false)
                        .created(LocalDate.of(2017, 2, 12))
                        .lastUpdated(LocalDate.of(2017, 4, 23))
                        .propertiesAdd(
                                new Property(
                                        UniParcCrossReference.PROPERTY_PROTEIN_NAME,
                                        "proteinName" + i))
                        .build();
        UniParcEntryBuilder builder = UniParcEntryBuilder.from(entry);
        builder.uniParcCrossReferencesAdd(xref1);
        builder.uniParcCrossReferencesAdd(xref2);
        return builder.build();
    }

    protected Stream<Arguments> getAllReturnedFields() {
        return ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.UNIPARC).getReturnFields()
                .stream()
                .map(returnField -> Arguments.of(returnField.getName(), returnField.getPaths()));
    }
}
