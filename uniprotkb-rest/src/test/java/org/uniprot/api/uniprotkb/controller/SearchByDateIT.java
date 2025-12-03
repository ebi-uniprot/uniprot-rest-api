package org.uniprot.api.uniprotkb.controller;

import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.uniprot.api.uniprotkb.controller.UniProtKBController.UNIPROTKB_RESOURCE;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.uniprot.api.uniprotkb.UniProtKBREST;
import org.uniprot.api.uniprotkb.common.repository.UniProtKBDataStoreTestConfig;
import org.uniprot.api.uniprotkb.common.repository.search.UniprotQueryRepository;
import org.uniprot.api.uniprotkb.common.repository.store.UniProtKBStoreClient;
import org.uniprot.core.flatfile.writer.LineType;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.datastore.voldemort.uniprot.VoldemortInMemoryUniprotEntryStore;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.spark.indexer.uniprot.converter.UniProtEntryConverter;

@ExtendWith(SpringExtension.class)
@ActiveProfiles(profiles = "offline")
@SpringBootTest(classes = {UniProtKBDataStoreTestConfig.class, UniProtKBREST.class})
@WebAppConfiguration
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SearchByDateIT {
    private static final String SEARCH_RESOURCE = UNIPROTKB_RESOURCE + "/search";
    private static final String UNIPROT_FLAT_FILE_ENTRY_PATH = "/it/P0A377.43.dat";
    private static final String DT_LINE =
            "DT   %s, integrated into UniProtKB/Swiss-Prot.\n"
                    + "DT   %s, sequence version 2.\n"
                    + "DT   %s, entry version 97.";
    private static final String ACCESSION1 = "Q197F4";
    private static final String CREATE_DATE1 = "01-OCT-1989";
    private static final String UPDATE_DATE1 = "07-FEB-2006";
    private static final String ACCESSION2 = "Q197F5";
    private static final String CREATE_DATE2 = "30-JUL-2003";
    private static final String UPDATE_DATE2 = "01-JAN-2013";
    private static final String ACCESSION3 = "Q197F6";
    private static final String CREATE_DATE3 = "15-MAR-1999";
    private static final String UPDATE_DATE3 = "27-OCT-2004";
    // entries that deliberately cross the GMT -> BST boundary
    // BST for 2014: 0100 @ 30 March - 0100 @ 26 October
    private static final String ACCESSION_GMT = "Q197F7";
    private static final String CREATE_DATE_GMT = "29-MAR-2014";
    private static final String UPDATE_DATE_GMT = "02-APR-2014";
    // this is dubiously BST because BST starts @ 1am on this day, and we specify no time
    private static final String ACCESSION_BST_DUBIOUS = "Q197F8";
    private static final String CREATE_DATE_BST_DUBIOUS = "30-MAR-2014";
    private static final String UPDATE_DATE_BST_DUBIOUS = "02-APR-2014";
    private static final String ACCESSION_BST = "Q197F9";
    private static final String CREATE_DATE_BST = "31-MAR-2014";
    private static final String UPDATE_DATE_BST = "02-APR-2014";
    private static final String ACC_LINE = "AC   %s;";
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    @RegisterExtension static DataStoreManager storeManager = new DataStoreManager();

    @Autowired private UniprotQueryRepository repository;

    @Autowired private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeAll
    void setUp() throws IOException {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        storeManager.addSolrClient(DataStoreManager.StoreType.UNIPROT, SolrCollection.uniprot);
        ReflectionTestUtils.setField(
                repository,
                "solrClient",
                storeManager.getSolrClient(DataStoreManager.StoreType.UNIPROT));

        UniProtEntryConverter uniProtEntryConverter =
                new UniProtEntryConverter(new HashMap<>(), new HashMap<>());

        storeManager.addDocConverter(DataStoreManager.StoreType.UNIPROT, uniProtEntryConverter);

        UniProtKBStoreClient storeClient =
                new UniProtKBStoreClient(
                        VoldemortInMemoryUniprotEntryStore.getInstance("avro-uniprot"));
        storeManager.addStore(DataStoreManager.StoreType.UNIPROT, storeClient);

        InputStream resourceAsStream = TestUtils.getResourceAsStream(UNIPROT_FLAT_FILE_ENTRY_PATH);
        UniProtEntryObjectProxy entryProxy =
                UniProtEntryObjectProxy.createEntryFromInputStream(resourceAsStream);

        // Entry 1
        entryProxy.updateEntryObject(LineType.AC, String.format(ACC_LINE, ACCESSION1));
        entryProxy.updateEntryObject(
                LineType.DT, String.format(DT_LINE, CREATE_DATE1, CREATE_DATE1, UPDATE_DATE1));
        UniProtKBEntry entry1 = TestUtils.convertToUniProtEntry(entryProxy);
        //     storeManager.save(DataStoreManager.StoreType.UNIPROT,
        // TestUtils.convertToUniProtEntry(entryProxy));

        // Entry 2
        entryProxy.updateEntryObject(LineType.AC, String.format(ACC_LINE, ACCESSION2));
        entryProxy.updateEntryObject(
                LineType.DT, String.format(DT_LINE, CREATE_DATE2, CREATE_DATE2, UPDATE_DATE2));
        UniProtKBEntry entry2 = TestUtils.convertToUniProtEntry(entryProxy);
        //      storeManager.save(DataStoreManager.StoreType.UNIPROT,
        // TestUtils.convertToUniProtEntry(entryProxy));

        // Entry 3
        entryProxy.updateEntryObject(LineType.AC, String.format(ACC_LINE, ACCESSION3));
        entryProxy.updateEntryObject(
                LineType.DT, String.format(DT_LINE, CREATE_DATE3, CREATE_DATE3, UPDATE_DATE3));
        UniProtKBEntry entry3 = TestUtils.convertToUniProtEntry(entryProxy);
        //     storeManager.save(DataStoreManager.StoreType.UNIPROT,
        // TestUtils.convertToUniProtEntry(entryProxy));

        // Entry 4
        entryProxy.updateEntryObject(LineType.AC, String.format(ACC_LINE, ACCESSION_BST));
        entryProxy.updateEntryObject(
                LineType.DT,
                String.format(DT_LINE, CREATE_DATE_BST, CREATE_DATE_BST, UPDATE_DATE_BST));
        UniProtKBEntry entry4 = TestUtils.convertToUniProtEntry(entryProxy);
        //    storeManager.save(DataStoreManager.StoreType.UNIPROT,
        // TestUtils.convertToUniProtEntry(entryProxy));

        // Entry 5
        entryProxy.updateEntryObject(LineType.AC, String.format(ACC_LINE, ACCESSION_GMT));
        entryProxy.updateEntryObject(
                LineType.DT,
                String.format(DT_LINE, CREATE_DATE_GMT, CREATE_DATE_GMT, UPDATE_DATE_GMT));
        UniProtKBEntry entry5 = TestUtils.convertToUniProtEntry(entryProxy);
        storeManager.save(
                DataStoreManager.StoreType.UNIPROT, entry1, entry2, entry3, entry4, entry5);
    }

    private String buildQuery(
            String queryTerm, String start, boolean includeStart, String end, boolean includeEnd) {
        StringBuilder sb = new StringBuilder();
        sb.append(queryTerm)
                .append(":")
                .append("[")
                .append(start)
                .append(" TO ")
                .append(end)
                .append("]");
        System.out.println(sb.toString());
        return sb.toString();
    }

    @Test
    void searchForCreatedBefore30SEP1989Returns0Documents() throws Exception {

        LocalDate creationDate = LocalDate.of(1989, 9, 30);

        String query =
                buildQuery(
                        getSearchField("date_created"),
                        "*",
                        false,
                        creationDate.atStartOfDay().format(DATE_FORMAT),
                        false);
        verifyTest(query, null);
    }

    private void verifyTest(String query, String... accessions) throws Exception {
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(SEARCH_RESOURCE)
                                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                .param("query", query));

        if (accessions == null) {
            // then
            response.andDo(MockMvcResultHandlers.log())
                    .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                    .andExpect(
                            MockMvcResultMatchers.header()
                                    .string(
                                            HttpHeaders.CONTENT_TYPE,
                                            MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(
                            MockMvcResultMatchers.jsonPath("$.results.*.primaryAccession")
                                    .doesNotExist());
        } else {
            response.andDo(MockMvcResultHandlers.log())
                    .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                    .andExpect(
                            MockMvcResultMatchers.header()
                                    .string(
                                            HttpHeaders.CONTENT_TYPE,
                                            MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(
                            MockMvcResultMatchers.jsonPath(
                                    "$.results.*.primaryAccession", contains(accessions)));
        }
    }

    @Test
    void searchForCreatedBefore01OCT1989Returns1Document() throws Exception {
        LocalDate creationDate = LocalDate.of(1989, 10, 1);
        String query =
                buildQuery(
                        getSearchField("date_created"),
                        "*",
                        false,
                        creationDate.atStartOfDay().format(DATE_FORMAT),
                        false);
        verifyTest(query, ACCESSION1);
    }

    @Test
    void searchForCreatedBefore15MAR1999Returns2Documents() throws Exception {
        LocalDate creationDate = LocalDate.of(1999, 3, 15);
        String query =
                buildQuery(
                        getSearchField("date_created"),
                        "*",
                        false,
                        creationDate.atStartOfDay().format(DATE_FORMAT),
                        false);
        verifyTest(query, ACCESSION1, ACCESSION3);
    }

    @Test
    void searchForUpdatedBefore26OCT2004Returns0Documents() throws Exception {
        LocalDate updateDate = LocalDate.of(2004, 10, 26);
        String query =
                buildQuery(
                        getSearchField("date_modified"),
                        "*",
                        false,
                        updateDate.atStartOfDay().format(DATE_FORMAT),
                        false);

        verifyTest(query, null);
    }

    @Test
    void searchForUpdatedBefore27OCT2004Returns1Documents() throws Exception {
        LocalDate updateDate = LocalDate.of(2004, 10, 27);
        String query =
                buildQuery(
                        getSearchField("date_modified"),
                        "*",
                        false,
                        updateDate.atStartOfDay().format(DATE_FORMAT),
                        false);

        verifyTest(query, ACCESSION3);
    }

    @Test
    void searchForUpdatedBefore08FEB2006Returns2Documents() throws Exception {
        LocalDate updateDate = LocalDate.of(2006, 2, 8);
        String query =
                buildQuery(
                        getSearchField("date_modified"),
                        "*",
                        false,
                        updateDate.atStartOfDay().format(DATE_FORMAT),
                        false);
        verifyTest(query, ACCESSION1, ACCESSION3);
    }

    @Test
    void searchForCreatedAfter31MAR2014Returns1Document() throws Exception {

        LocalDate creationDate = LocalDate.of(2014, 3, 30);
        String query =
                buildQuery(
                        getSearchField("date_created"),
                        creationDate.atStartOfDay().format(DATE_FORMAT),
                        false,
                        "*",
                        false);
        verifyTest(query, ACCESSION_BST);
    }

    @Test
    void searchForCreatedAfter30JUL2003Returns3Documents() throws Exception {
        LocalDate creationDate = LocalDate.of(2003, 7, 29);
        String query =
                buildQuery(
                        getSearchField("date_created"),
                        creationDate.atStartOfDay().format(DATE_FORMAT),
                        false,
                        "*",
                        false);
        verifyTest(query, ACCESSION2, ACCESSION_GMT, ACCESSION_BST);
    }

    @Test
    void searchForCreatedAfter15MAR1999Returns2Documents() throws Exception {

        LocalDate creationDate = LocalDate.of(1999, 3, 15);
        String query =
                buildQuery(
                        getSearchField("date_created"),
                        creationDate.atStartOfDay().format(DATE_FORMAT),
                        true,
                        "*",
                        true);
        verifyTest(query, ACCESSION2, ACCESSION3, ACCESSION_GMT, ACCESSION_BST);
    }

    @Test
    void searchForUpdatedAfter02APR2014Returns3Documents() throws Exception {
        LocalDate updateDate = LocalDate.of(2014, 4, 1);
        String query =
                buildQuery(
                        getSearchField("date_modified"),
                        updateDate.atStartOfDay().format(DATE_FORMAT),
                        true,
                        "*",
                        true);
        verifyTest(query, ACCESSION_GMT, ACCESSION_BST);
    }

    @Test
    void searchForUpdatedAfter01JAN2013Returns3Documents() throws Exception {
        LocalDate updateDate = LocalDate.of(2012, 12, 31);
        String query =
                buildQuery(
                        getSearchField("date_modified"),
                        updateDate.atStartOfDay().format(DATE_FORMAT),
                        true,
                        "*",
                        true);
        verifyTest(query, ACCESSION2, ACCESSION_GMT, ACCESSION_BST);
    }

    @Test
    void searchForUpdatedAfter07FEB2006Returns4Documents() throws Exception {
        LocalDate updateDate = LocalDate.of(2006, 2, 6);
        String query =
                buildQuery(
                        getSearchField("date_modified"),
                        updateDate.atStartOfDay().format(DATE_FORMAT),
                        true,
                        "*",
                        true);
        verifyTest(query, ACCESSION1, ACCESSION2, ACCESSION_GMT, ACCESSION_BST);
    }

    @Test
    void searchCreationBetween20FEB1979And10Dec1979Returns0Documents() throws Exception {
        LocalDate startDate = LocalDate.of(1979, 2, 20);
        LocalDate endDate = LocalDate.of(1979, 12, 10);

        String query =
                buildQuery(
                        getSearchField("date_created"),
                        startDate.atStartOfDay().format(DATE_FORMAT),
                        true,
                        endDate.atStartOfDay().format(DATE_FORMAT),
                        true);
        verifyTest(query, null);
    }

    @Test
    void createdBetween01JAN1989And01JAN2000ReturnsEntry1And3() throws Exception {
        LocalDate startDate = LocalDate.of(1989, 1, 1);
        LocalDate endDate = LocalDate.of(2000, 1, 1);

        String query =
                buildQuery(
                        getSearchField("date_created"),
                        startDate.atStartOfDay().format(DATE_FORMAT),
                        true,
                        endDate.atStartOfDay().format(DATE_FORMAT),
                        true);
        verifyTest(query, ACCESSION1, ACCESSION3);
    }

    @Test
    void searchUpdateBetween20FEB1979And10Dec1979Returns0Documents() throws Exception {
        LocalDate startDate = LocalDate.of(1979, 2, 20);
        LocalDate endDate = LocalDate.of(1979, 12, 10);

        String query =
                buildQuery(
                        getSearchField("date_modified"),
                        startDate.atStartOfDay().format(DATE_FORMAT),
                        true,
                        endDate.atStartOfDay().format(DATE_FORMAT),
                        true);
        verifyTest(query, null);
    }

    @Test
    void updatedBetween01JAN2004And01JAN2006ReturnsEntry1And3() throws Exception {
        LocalDate startDate = LocalDate.of(2004, 1, 1);
        LocalDate endDate = LocalDate.of(2006, 12, 1);

        String query =
                buildQuery(
                        getSearchField("date_modified"),
                        startDate.atStartOfDay().format(DATE_FORMAT),
                        true,
                        endDate.atStartOfDay().format(DATE_FORMAT),
                        true);
        verifyTest(query, ACCESSION1, ACCESSION3);
    }

    /*
     * BST for 2014:    30 March - 26 October
     * Entry created:   29 March 2014, therefore this date is GMT
     */
    @Test
    void searchExplicitGMTEntryTestUpperBound() throws Exception {
        LocalDate startDate = LocalDate.of(2014, 3, 28);
        LocalDate endDate = LocalDate.of(2014, 3, 29);

        String query =
                buildQuery(
                        getSearchField("date_created"),
                        startDate.atStartOfDay().format(DATE_FORMAT),
                        true,
                        endDate.atStartOfDay().format(DATE_FORMAT),
                        true);
        verifyTest(query, ACCESSION_GMT);
    }

    @Test
    void searchExplicitGMTEntryTestExactDay() throws Exception {
        LocalDate startDate = LocalDate.of(2014, 3, 29);
        LocalDate endDate = LocalDate.of(2014, 3, 29);

        String query =
                buildQuery(
                        getSearchField("date_created"),
                        startDate.atStartOfDay().format(DATE_FORMAT),
                        true,
                        endDate.atStartOfDay().format(DATE_FORMAT),
                        true);
        verifyTest(query, ACCESSION_GMT);
    }

    @Test
    void searchExplicitGMTEntryTestOver() throws Exception {
        LocalDate startDate = LocalDate.of(2014, 3, 28);
        LocalDate endDate = LocalDate.of(2014, 3, 30);

        String query =
                buildQuery(
                        getSearchField("date_created"),
                        startDate.atStartOfDay().format(DATE_FORMAT),
                        true,
                        endDate.atStartOfDay().format(DATE_FORMAT),
                        true);
        verifyTest(query, ACCESSION_GMT);
    }

    @Test
    void searchExplicitGMTEntryTestLowerBound() throws Exception {
        LocalDate startDate = LocalDate.of(2014, 3, 29);
        LocalDate endDate = LocalDate.of(2014, 3, 30);

        String query =
                buildQuery(
                        getSearchField("date_created"),
                        startDate.atStartOfDay().format(DATE_FORMAT),
                        true,
                        endDate.atStartOfDay().format(DATE_FORMAT),
                        true);
        verifyTest(query, ACCESSION_GMT);
    }

    /*
     * ---------------------------------------------------------------------------------------------
     * BST for 2014:    0100 @ 30 March - 0100 @ 26 October
     * Entry created:   31 March 2014, therefore this date is GMT
     */
    @Test
    void searchExplicitBSTEntryTestUpperBound() throws Exception {
        LocalDate startDate = LocalDate.of(2014, 3, 30);
        LocalDate endDate = LocalDate.of(2014, 3, 31);

        String query =
                buildQuery(
                        getSearchField("date_created"),
                        startDate.atStartOfDay().format(DATE_FORMAT),
                        true,
                        endDate.atStartOfDay().format(DATE_FORMAT),
                        true);
        verifyTest(query, ACCESSION_BST);
    }

    @Test
    void searchExplicitBSTEntryTestExactDay() throws Exception {
        LocalDate startDate = LocalDate.of(2014, 3, 30);
        LocalDate endDate = LocalDate.of(2014, 3, 31);

        String query =
                buildQuery(
                        getSearchField("date_created"),
                        startDate.atStartOfDay().format(DATE_FORMAT),
                        true,
                        endDate.atStartOfDay().format(DATE_FORMAT),
                        true);
        verifyTest(query, ACCESSION_BST);
    }

    @Test
    void searchExplicitBSTEntryTestOver() throws Exception {
        LocalDate startDate = LocalDate.of(2014, 3, 30);
        LocalDate endDate = LocalDate.of(2014, 4, 1);

        String query =
                buildQuery(
                        getSearchField("date_created"),
                        startDate.atStartOfDay().format(DATE_FORMAT),
                        true,
                        endDate.atStartOfDay().format(DATE_FORMAT),
                        true);
        verifyTest(query, ACCESSION_BST);
    }

    @Test
    void searchExplicitBSTEntryTestLowerBound() throws Exception {
        LocalDate startDate = LocalDate.of(2014, 3, 30);
        LocalDate endDate = LocalDate.of(2014, 4, 1);

        String query =
                buildQuery(
                        getSearchField("date_created"),
                        startDate.atStartOfDay().format(DATE_FORMAT),
                        true,
                        endDate.atStartOfDay().format(DATE_FORMAT),
                        true);
        verifyTest(query, ACCESSION_BST);
    }

    private String getSearchField(String name) {
        SearchFieldConfig searchFieldConfig =
                SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIPROTKB);
        return searchFieldConfig.getSearchFieldItemByName(name).getFieldName();
    }
}
