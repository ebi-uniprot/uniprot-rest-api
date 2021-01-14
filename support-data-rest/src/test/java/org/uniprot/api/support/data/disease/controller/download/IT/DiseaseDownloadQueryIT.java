package org.uniprot.api.support.data.disease.controller.download.IT;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.rest.controller.param.DownloadParamAndResult;
import org.uniprot.api.support.data.DataStoreTestConfig;
import org.uniprot.api.support.data.SupportDataRestApplication;
import org.uniprot.api.support.data.disease.controller.DiseaseController;
import org.uniprot.api.support.data.disease.controller.download.resolver.DiseaseDownloadQueryParamAndResultProvider;

import edu.emory.mathcs.backport.java.util.Arrays;

/** Class to test download api with query */
@ContextConfiguration(classes = {DataStoreTestConfig.class, SupportDataRestApplication.class})
@ActiveProfiles(profiles = "offline")
@AutoConfigureWebClient
@WebMvcTest(DiseaseController.class)
@ExtendWith(value = {SpringExtension.class})
public class DiseaseDownloadQueryIT extends BaseDiseaseDownloadIT {
    private static final String SOLR_QUERY = "id:" + ACC1;
    private static final String BAD_SOLR_QUERY = "random_field:disease";

    @RegisterExtension
    static DiseaseDownloadQueryParamAndResultProvider paramAndResultProvider =
            new DiseaseDownloadQueryParamAndResultProvider();

    @BeforeEach
    public void setUpData() {
        // when
        saveEntry(ACC1, 1);
        saveEntry(ACC2, 2);
        saveEntry(ACC3, 3);
    }

    @ParameterizedTest(name = "[{index}]~/download?{0}")
    @MethodSource("provideRequestResponseByType")
    void testDownloadByAccession(DownloadParamAndResult paramAndResult) throws Exception {
        sendAndVerify(paramAndResult, HttpStatus.OK);
    }

    @ParameterizedTest(name = "[{index}]~/download?{0}")
    @MethodSource("provideRequestResponseByTypeWithoutQuery")
    void testDownloadWithoutQuery(DownloadParamAndResult paramAndResult) throws Exception {
        sendAndVerify(paramAndResult, HttpStatus.BAD_REQUEST);
    }

    @ParameterizedTest(name = "[{index}]~/download?{0}")
    @MethodSource("provideRequestResponseByTypeWithBadQuery")
    void testDownloadWithBadQuery(DownloadParamAndResult paramAndResult) throws Exception {
        sendAndVerify(paramAndResult, HttpStatus.BAD_REQUEST);
    }

    private static Stream<Arguments> provideRequestResponseByType() {
        return getSupportedContentTypes().stream()
                .map(
                        type ->
                                Arguments.of(
                                        paramAndResultProvider.getDownloadParamAndResultForQuery(
                                                type,
                                                1,
                                                SOLR_QUERY,
                                                Arrays.asList(new String[] {ACC2}))));
    }

    private static Stream<Arguments> provideRequestResponseByTypeWithoutQuery() {
        return getSupportedContentTypes().stream()
                .map(
                        type ->
                                Arguments.of(
                                        paramAndResultProvider.getDownloadParamAndResultForQuery(
                                                type, null, null, null)));
    }

    private static Stream<Arguments> provideRequestResponseByTypeWithBadQuery() {
        return getSupportedContentTypes().stream()
                .map(
                        type ->
                                Arguments.of(
                                        paramAndResultProvider.getDownloadParamAndResultForQuery(
                                                type, null, BAD_SOLR_QUERY, null)));
    }
}
