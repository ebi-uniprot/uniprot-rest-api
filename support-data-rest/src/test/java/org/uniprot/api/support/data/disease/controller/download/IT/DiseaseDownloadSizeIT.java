package org.uniprot.api.support.data.disease.controller.download.IT;

import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.rest.controller.param.DownloadParamAndResult;
import org.uniprot.api.support.data.DataStoreTestConfig;
import org.uniprot.api.support.data.SupportDataRestApplication;
import org.uniprot.api.support.data.disease.controller.DiseaseController;
import org.uniprot.api.support.data.disease.controller.download.resolver.DiseaseDownloadSizeParamAndResultProvider;

/** Class to test download api with certain size.. */
@ContextConfiguration(classes = {DataStoreTestConfig.class, SupportDataRestApplication.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(DiseaseController.class)
@ExtendWith(value = {SpringExtension.class})
public class DiseaseDownloadSizeIT extends BaseDiseaseDownloadIT {

    @RegisterExtension
    static DiseaseDownloadSizeParamAndResultProvider paramAndResultProvider =
            new DiseaseDownloadSizeParamAndResultProvider();

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
                                Arguments.of(
                                        paramAndResultProvider.getDownloadParamAndResult(
                                                type, LESS_THAN_ZERO_SIZE)));
    }

    private static Stream<Arguments> provideRequestResponseByTypeBatchSize() {
        return getSupportedContentTypes().stream()
                .map(
                        type ->
                                Arguments.of(
                                        paramAndResultProvider.getDownloadParamAndResult(
                                                type, BATCH_SIZE)));
    }

    private static Stream<Arguments> provideRequestResponseByTypeMoreBatchSize() {
        return getSupportedContentTypes().stream()
                .map(
                        type ->
                                Arguments.of(
                                        paramAndResultProvider.getDownloadParamAndResult(
                                                type, MORE_THAN_BATCH_SIZE)));
    }

    private static Stream<Arguments> provideRequestResponseByTypeLessBatchSize() {
        return getSupportedContentTypes().stream()
                .map(
                        type ->
                                Arguments.of(
                                        paramAndResultProvider.getDownloadParamAndResult(
                                                type, LESS_THAN_BATCH_SIZE)));
    }
}
