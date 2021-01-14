package org.uniprot.api.support.data.disease.controller.download.IT;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.rest.controller.param.DownloadParamAndResult;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.support.data.DataStoreTestConfig;
import org.uniprot.api.support.data.SupportDataRestApplication;
import org.uniprot.api.support.data.disease.controller.DiseaseController;
import org.uniprot.api.support.data.disease.controller.download.resolver.DiseaseDownloadSortParamAndResultProvider;

@ContextConfiguration(classes = {DataStoreTestConfig.class, SupportDataRestApplication.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(DiseaseController.class)
@ExtendWith(value = {SpringExtension.class})
public class DiseaseDownloadSortIT extends BaseDiseaseDownloadIT {
    public static List<String> SORTED_BY_ACCESSION = Arrays.asList(ACC2, ACC1, ACC3);

    @RegisterExtension
    static DiseaseDownloadSortParamAndResultProvider paramAndResultProvider =
            new DiseaseDownloadSortParamAndResultProvider();

    @BeforeEach
    public void setUpData() {
        // when
        saveEntry(ACC1, 1);
        saveEntry(ACC2, 2);
        saveEntry(ACC3, 3);
    }

    @ParameterizedTest(name = "[{index}]~/download?{0}")
    @MethodSource("accession")
    void testDownloadSortByAccession(DownloadParamAndResult paramAndResult) throws Exception {
        sendAndVerify(paramAndResult, HttpStatus.OK);
    }

    private static Stream<Arguments> accession() {
        return requestResponseForSort("id", "asc", SORTED_BY_ACCESSION);
    }

    private static Stream<Arguments> requestResponseForSort(
            String fieldName, String sortOrder, List<String> accessionsOrder) {
        return Stream.of(
                Arguments.of(
                        getParamAndResult(
                                UniProtMediaType.TSV_MEDIA_TYPE,
                                fieldName,
                                sortOrder,
                                accessionsOrder)),
                Arguments.of(
                        getParamAndResult(
                                UniProtMediaType.LIST_MEDIA_TYPE,
                                fieldName,
                                sortOrder,
                                accessionsOrder)),
                Arguments.of(
                        getParamAndResult(
                                MediaType.APPLICATION_JSON, fieldName, sortOrder, accessionsOrder)),
                Arguments.of(
                        getParamAndResult(
                                UniProtMediaType.XLS_MEDIA_TYPE,
                                fieldName,
                                sortOrder,
                                accessionsOrder)),
                Arguments.of(
                        getParamAndResult(
                                UniProtMediaType.OBO_MEDIA_TYPE,
                                fieldName,
                                sortOrder,
                                accessionsOrder)));
    }

    private static DownloadParamAndResult getParamAndResult(
            MediaType mediaType, String fieldName, String sortOrder, List<String> accessionsOrder) {
        return paramAndResultProvider.getDownloadParamAndResultForSort(
                mediaType, fieldName, sortOrder, 3, accessionsOrder);
    }
}
