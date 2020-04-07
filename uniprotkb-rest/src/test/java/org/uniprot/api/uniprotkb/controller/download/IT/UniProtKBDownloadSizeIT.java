package org.uniprot.api.uniprotkb.controller.download.IT;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
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
import org.uniprot.api.uniprotkb.controller.download.resolver.UniProtKBDownloadSizeParamAndResultProvider;
import org.uniprot.api.uniprotkb.repository.DataStoreTestConfig;

/** Class to test download api with certain size.. */
@ContextConfiguration(classes = {DataStoreTestConfig.class, UniProtKBREST.class})
@ActiveProfiles(profiles = "offline")
@AutoConfigureWebClient
@WebMvcTest(UniprotKBController.class)
@ExtendWith(value = {SpringExtension.class})
public class UniProtKBDownloadSizeIT extends BaseUniprotKBDownloadIT {
    @RegisterExtension
    static UniProtKBDownloadSizeParamAndResultProvider paramAndResultProvider =
            new UniProtKBDownloadSizeParamAndResultProvider();

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

    @ParameterizedTest(name = "[{index}]~/download?{0}")
    @MethodSource("provideRequestResponseByTypeLessBatchSize")
    void testDownloadLessThanBatchSize(DownloadParamAndResult paramAndResult) throws Exception {

        sendAndVerify(paramAndResult, HttpStatus.OK);
    }

    @ParameterizedTest(name = "[{index}]~/download?{0}")
    @MethodSource("provideRequestResponseByTypeMoreBatchSize")
    void testDownloadMoreThanBatchSize(DownloadParamAndResult paramAndResult) throws Exception {

        sendAndVerify(paramAndResult, HttpStatus.OK);
    }

    @ParameterizedTest(name = "[{index}]~/download?{0}")
    @MethodSource("provideRequestResponseByTypeBatchSize")
    void testDownloadDefaultBatchSize(DownloadParamAndResult paramAndResult) throws Exception {

        sendAndVerify(paramAndResult, HttpStatus.OK);
    }

    @ParameterizedTest(name = "[{index}]~/download?{0}")
    @MethodSource("provideRequestResponseByTypeNegativeBatchSize")
    void testDownloadNegativeBatchSize(DownloadParamAndResult paramAndResult) throws Exception {

        sendAndVerify(paramAndResult, HttpStatus.BAD_REQUEST);
    }

    private static Stream<Arguments> provideRequestResponseByTypeNegativeBatchSize() {
        return getSupportedContentTypes().stream()
                .map(
                        type ->
                                paramAndResultProvider.getDownloadParamAndResult(
                                        type, LESS_THAN_ZERO_SIZE))
                .map(param -> Arguments.of(param));
    }

    private static Stream<Arguments> provideRequestResponseByTypeBatchSize() {
        return getSupportedContentTypes().stream()
                .map(type -> paramAndResultProvider.getDownloadParamAndResult(type, BATCH_SIZE))
                .map(param -> Arguments.of(param));
    }

    private static Stream<Arguments> provideRequestResponseByTypeMoreBatchSize() {
        return getSupportedContentTypes().stream()
                .map(
                        type ->
                                paramAndResultProvider.getDownloadParamAndResult(
                                        type, MORE_THAN_BATCH_SIZE))
                .map(param -> Arguments.of(param));
    }

    private static Stream<Arguments> provideRequestResponseByTypeLessBatchSize() {
        return getSupportedContentTypes().stream()
                .map(
                        type ->
                                paramAndResultProvider.getDownloadParamAndResult(
                                        type, LESS_THAN_BATCH_SIZE))
                .map(param -> Arguments.of(param));
    }
}
