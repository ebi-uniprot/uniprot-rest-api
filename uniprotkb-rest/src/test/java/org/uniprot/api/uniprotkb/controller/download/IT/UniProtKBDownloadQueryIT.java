package org.uniprot.api.uniprotkb.controller.download.IT;

import edu.emory.mathcs.backport.java.util.Arrays;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.uniprot.api.rest.controller.param.DownloadParamAndResult;
import org.uniprot.api.uniprotkb.UniProtKBREST;
import org.uniprot.api.uniprotkb.controller.UniprotKBController;
import org.uniprot.api.uniprotkb.controller.download.resolver.UniProtKBDownloadQueryParamAndResultProvider;
import org.uniprot.api.uniprotkb.repository.DataStoreTestConfig;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.stream.Stream;

/**
 * Class to test download api with query
 */
@ContextConfiguration(classes = {DataStoreTestConfig.class, UniProtKBREST.class})
@ActiveProfiles(profiles = "offline")
@AutoConfigureWebClient
@WebMvcTest(UniprotKBController.class)
@ExtendWith(value = {SpringExtension.class})
public class UniProtKBDownloadQueryIT extends BaseUniprotKBDownloadIT {
    private static final String SOLR_QUERY = "accession:" + ACC2;
    private static final String BAD_SOLR_QUERY = "random_field:protein";

    @RegisterExtension
    static UniProtKBDownloadQueryParamAndResultProvider paramAndResultProvider =
            new UniProtKBDownloadQueryParamAndResultProvider();

    private static final String RDF_TEST_FILE = "src/test/resources/downloadIT/P12345.rdf";

    @Qualifier("rdfRestTemplate")
    @Autowired
    private RestTemplate restTemplate;

    @BeforeAll
    void setUp() {
        super.setUp();
        String rdfString = null;
        try {
            rdfString = FileUtils.readFileToString(new File(RDF_TEST_FILE), "UTF-8");
        } catch (IOException e) {
            Assertions.fail(e);
        }
        DefaultUriBuilderFactory urilBuilderFactory = new DefaultUriBuilderFactory();
        Mockito.when(this.restTemplate.getUriTemplateHandler()).thenReturn(urilBuilderFactory);
        Mockito.when(
                        this.restTemplate.getForObject(
                                Mockito.any(URI.class), Mockito.eq(String.class)))
                .thenReturn(rdfString);
    }

    @BeforeEach
    public void setUpData() {
        // when
        saveEntry(ACC1, 1);
        saveEntry(ACC2, 2);
        saveEntry(ACC3, 3);
    }

    @ParameterizedTest(name = "[{index}]~/download?{0}")
    @MethodSource("provideRequestResponseByType")
    void testDownloadByAccession(DownloadParamAndResult paramAndResult)
            throws Exception {
        sendAndVerify(paramAndResult, HttpStatus.OK);
    }


    @ParameterizedTest(name = "[{index}]~/download?{0}")
    @MethodSource("provideRequestResponseByTypeWithoutQuery")
    void testDownloadWithoutQuery(DownloadParamAndResult paramAndResult)
            throws Exception {
        sendAndVerify(paramAndResult, HttpStatus.BAD_REQUEST);
    }

    @ParameterizedTest(name = "[{index}]~/download?{0}")
    @MethodSource("provideRequestResponseByTypeWithBadQuery")
    void testDownloadWithBadQuery(DownloadParamAndResult paramAndResult)
            throws Exception {
        sendAndVerify(paramAndResult, HttpStatus.BAD_REQUEST);
    }

    private static Stream<Arguments> provideRequestResponseByType() {
        return getSupportedContentTypes().stream()
                .map(type ->
                        Arguments.of(paramAndResultProvider
                                .getDownloadParamAndResultForQuery(type, 1, SOLR_QUERY, Arrays.asList(new String[]{ACC2}))));
    }

    private static Stream<Arguments> provideRequestResponseByTypeWithoutQuery() {
        return getSupportedContentTypes().stream()
                .map(type ->
                        Arguments.of(paramAndResultProvider
                                .getDownloadParamAndResultForQuery(type, null, null,null)));
    }

    private static Stream<Arguments> provideRequestResponseByTypeWithBadQuery() {
        return getSupportedContentTypes().stream()
                .map(type ->
                        Arguments.of(paramAndResultProvider
                                .getDownloadParamAndResultForQuery(type, null, BAD_SOLR_QUERY,null)));
    }
}
