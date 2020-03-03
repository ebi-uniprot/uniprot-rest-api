package org.uniprot.api.disease.download.IT;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
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
import org.uniprot.api.DataStoreTestConfig;
import org.uniprot.api.disease.DiseaseController;
import org.uniprot.api.disease.download.resolver.DiseaseDownloadSizeParamResolver;
import org.uniprot.api.rest.controller.param.DownloadParamAndResult;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.support_data.SupportDataApplication;

/** Class to test download api with certain size.. */
@ContextConfiguration(classes = {DataStoreTestConfig.class, SupportDataApplication.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(DiseaseController.class)
@ExtendWith(value = {SpringExtension.class})
public class DiseaseDownloadSizeIT extends BaseDiseaseDownloadIT {
    @RegisterExtension
    static DiseaseDownloadSizeParamResolver paramResolver = new DiseaseDownloadSizeParamResolver();

    @Test
    protected void testDownloadLessThanDefaultBatchSizeJSON(DownloadParamAndResult paramAndResult)
            throws Exception {
        sendAndVerify(paramAndResult, HttpStatus.OK);
    }

    @Test
    protected void testDownloadDefaultBatchSizeJSON(DownloadParamAndResult paramAndResult)
            throws Exception {
        sendAndVerify(paramAndResult, HttpStatus.OK);
    }

    @Test
    protected void testDownloadMoreThanDefaultBatchSizeJSON(DownloadParamAndResult paramAndResult)
            throws Exception {
        sendAndVerify(paramAndResult, HttpStatus.OK);
    }

    @Test
    protected void testDownloadSizeLessThanZeroJSON(DownloadParamAndResult paramAndResult)
            throws Exception {
        sendAndVerify(paramAndResult, HttpStatus.BAD_REQUEST);
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
        return Stream.of(
                Arguments.of(
                        paramResolver.getDownloadSizeLessThanZeroParamAndResult(
                                UniProtMediaType.TSV_MEDIA_TYPE)),
                Arguments.of(
                        paramResolver.getDownloadSizeLessThanZeroParamAndResult(
                                UniProtMediaType.LIST_MEDIA_TYPE)),
                Arguments.of(
                        paramResolver.getDownloadSizeLessThanZeroParamAndResult(
                                UniProtMediaType.XLS_MEDIA_TYPE)),
                Arguments.of(
                        paramResolver.getDownloadSizeLessThanZeroParamAndResult(
                                UniProtMediaType.OBO_MEDIA_TYPE)));
    }

    private static Stream<Arguments> provideRequestResponseByTypeBatchSize() {
        return Stream.of(
                Arguments.of(
                        paramResolver.getDownloadDefaultBatchSizeParamAndResult(
                                UniProtMediaType.TSV_MEDIA_TYPE)),
                Arguments.of(
                        paramResolver.getDownloadDefaultBatchSizeParamAndResult(
                                UniProtMediaType.LIST_MEDIA_TYPE)),
                Arguments.of(
                        paramResolver.getDownloadDefaultBatchSizeParamAndResult(
                                UniProtMediaType.XLS_MEDIA_TYPE)),
                Arguments.of(
                        paramResolver.getDownloadDefaultBatchSizeParamAndResult(
                                UniProtMediaType.OBO_MEDIA_TYPE)));
    }

    private static Stream<Arguments> provideRequestResponseByTypeMoreBatchSize() {
        return Stream.of(
                Arguments.of(
                        paramResolver.getDownloadMoreThanBatchSizeParamAndResult(
                                UniProtMediaType.TSV_MEDIA_TYPE)),
                Arguments.of(
                        paramResolver.getDownloadMoreThanBatchSizeParamAndResult(
                                UniProtMediaType.LIST_MEDIA_TYPE)),
                Arguments.of(
                        paramResolver.getDownloadMoreThanBatchSizeParamAndResult(
                                UniProtMediaType.XLS_MEDIA_TYPE)),
                Arguments.of(
                        paramResolver.getDownloadMoreThanBatchSizeParamAndResult(
                                UniProtMediaType.OBO_MEDIA_TYPE)));
    }

    private static Stream<Arguments> provideRequestResponseByTypeLessBatchSize() {
        return Stream.of(
                Arguments.of(
                        paramResolver.getDownloadLessThanDefaultBatchSizeParamAndResult(
                                UniProtMediaType.TSV_MEDIA_TYPE)),
                Arguments.of(
                        paramResolver.getDownloadLessThanDefaultBatchSizeParamAndResult(
                                UniProtMediaType.LIST_MEDIA_TYPE)),
                Arguments.of(
                        paramResolver.getDownloadLessThanDefaultBatchSizeParamAndResult(
                                UniProtMediaType.XLS_MEDIA_TYPE)),
                Arguments.of(
                        paramResolver.getDownloadLessThanDefaultBatchSizeParamAndResult(
                                UniProtMediaType.OBO_MEDIA_TYPE)));
    }
}
