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
import org.uniprot.api.support.data.disease.controller.download.resolver.DiseaseDownloadParamAndResultProvider;

/** Class to test download api without any filter.. kind of download everything */
@ContextConfiguration(classes = {DataStoreTestConfig.class, SupportDataRestApplication.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(DiseaseController.class)
@ExtendWith(value = {SpringExtension.class})
public class DiseaseDownloadAllIT extends BaseDiseaseDownloadIT {
    @RegisterExtension
    static DiseaseDownloadParamAndResultProvider paramAndResultProvider =
            new DiseaseDownloadParamAndResultProvider();

    @ParameterizedTest(name = "[{index}]~/download?{0}")
    @MethodSource("provideRequestResponseByType")
    void testDownloadAll(DownloadParamAndResult paramAndResult) throws Exception {
        sendAndVerify(paramAndResult, HttpStatus.OK);
    }

    private static Stream<Arguments> provideRequestResponseByType() {
        return getSupportedContentTypes().stream()
                .map(type -> paramAndResultProvider.getDownloadParamAndResult(type, ENTRY_COUNT))
                .map(paramAndResult -> Arguments.of(paramAndResult));
    }
}
