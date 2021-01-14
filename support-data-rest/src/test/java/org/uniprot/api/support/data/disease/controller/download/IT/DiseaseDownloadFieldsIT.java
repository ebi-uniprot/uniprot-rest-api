package org.uniprot.api.support.data.disease.controller.download.IT;

import java.util.Arrays;
import java.util.Collections;
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
import org.uniprot.api.support.data.disease.controller.download.resolver.DiseaseDownloadFieldsParamAndResultProvider;

@ContextConfiguration(classes = {DataStoreTestConfig.class, SupportDataRestApplication.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(DiseaseController.class)
@ExtendWith(value = {SpringExtension.class})
public class DiseaseDownloadFieldsIT extends BaseDiseaseDownloadIT {
    private static final List<String> TSV_DEFAULT_FIELDS =
            Arrays.asList("Name", "DiseaseEntry ID", "Mnemonic", "Description");
    private static final List<String> XLS_DEFAULT_FIELDS =
            Arrays.asList("Name", "DiseaseEntry ID", "Mnemonic", "Description");
    private static final List<String> INVALID_RETURN_FIELDS =
            Collections.singletonList("embl,ebi,cross_references,reviewed_protein_count");
    private static final List<String> NON_DEFAULT_FIELDS =
            Arrays.asList(
                    "alternative_names", "cross_references", "keywords", "reviewed_protein_count");
    private static final List<String> NON_DEFAULT_RETURNED_FIELDS =
            Arrays.asList(
                    "Alternative Names",
                    "Cross Reference",
                    "Keywords",
                    "UniProtKB reviewed (Swiss-Prot) protein count");
    private static final List<String> NON_DEFAULT_RETURNED_FIELDS_JSON =
            Arrays.asList(
                    "alternativeNames", "crossReferences", "keywords", "reviewedProteinCount");

    @RegisterExtension
    static DiseaseDownloadFieldsParamAndResultProvider paramAndResultProvider =
            new DiseaseDownloadFieldsParamAndResultProvider();

    @BeforeEach
    public void setUpData() {
        // when
        saveEntry(ACC1, 1);
    }

    @ParameterizedTest(name = "[{index}]~/download?{0}")
    @MethodSource("provideRequestResponseDefaultFields")
    void testDownloadDefaultFields(DownloadParamAndResult paramAndResult) throws Exception {
        sendAndVerify(paramAndResult, HttpStatus.OK);
    }

    @ParameterizedTest(name = "[{index}]~/download?{0}")
    @MethodSource("paramAndResultByTypeForNonDefault")
    protected void testDownloadNonDefaultFieldsJSON(DownloadParamAndResult paramAndResult)
            throws Exception {
        sendAndVerify(paramAndResult, HttpStatus.OK);
    }

    @ParameterizedTest(name = "[{index}]~/download?{0}")
    @MethodSource("paramAndResultByTypeForInvalid")
    protected void testDownloadInvalidFields(DownloadParamAndResult paramAndResult)
            throws Exception {
        sendAndVerify(paramAndResult, HttpStatus.BAD_REQUEST);
    }

    private static Stream<Arguments> paramAndResultByTypeForNonDefault() {
        return Stream.of(
                Arguments.of(
                        getParamAndResult(
                                UniProtMediaType.TSV_MEDIA_TYPE,
                                NON_DEFAULT_FIELDS,
                                NON_DEFAULT_RETURNED_FIELDS)),
                Arguments.of(
                        getParamAndResult(
                                UniProtMediaType.XLS_MEDIA_TYPE,
                                NON_DEFAULT_FIELDS,
                                NON_DEFAULT_RETURNED_FIELDS)),
                Arguments.of(
                        getParamAndResult(
                                MediaType.APPLICATION_JSON,
                                NON_DEFAULT_FIELDS,
                                NON_DEFAULT_RETURNED_FIELDS_JSON)));
    }

    private static Stream<Arguments> provideRequestResponseDefaultFields() {
        return Stream.of(
                Arguments.of(
                        getParamAndResult(
                                UniProtMediaType.LIST_MEDIA_TYPE, null, Collections.emptyList())),
                Arguments.of(
                        getParamAndResult(
                                UniProtMediaType.OBO_MEDIA_TYPE, null, Collections.emptyList())),
                Arguments.of(
                        getParamAndResult(
                                MediaType.APPLICATION_JSON, null, Collections.emptyList())),
                Arguments.of(
                        getParamAndResult(
                                UniProtMediaType.TSV_MEDIA_TYPE, null, TSV_DEFAULT_FIELDS)),
                Arguments.of(
                        getParamAndResult(
                                UniProtMediaType.XLS_MEDIA_TYPE, null, XLS_DEFAULT_FIELDS)));
    }

    private static DownloadParamAndResult getParamAndResult(
            MediaType mediaType, List<String> reqFields, List<String> respFields) {
        return getParamAndResult(mediaType, Arrays.asList(ACC1), reqFields, respFields);
    }

    private static DownloadParamAndResult getParamAndResult(
            MediaType mediaType,
            List<String> accessions,
            List<String> reqFields,
            List<String> respFields) {
        return paramAndResultProvider.getDownloadParamAndResultForFields(
                mediaType, 1, accessions, reqFields, respFields);
    }

    private static Stream<Arguments> paramAndResultByTypeForInvalid() {
        return getSupportedContentTypes().stream()
                .map(
                        type ->
                                getParamAndResult(
                                        type, null, INVALID_RETURN_FIELDS, Collections.emptyList()))
                .map(paramAndResult -> Arguments.of(paramAndResult));
    }
}
