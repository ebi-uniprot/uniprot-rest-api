package org.uniprot.api.uniprotkb.controller.download.IT;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.uniprot.api.rest.controller.param.DownloadParamAndResult;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.uniprotkb.UniProtKBREST;
import org.uniprot.api.uniprotkb.controller.UniProtKBController;
import org.uniprot.api.uniprotkb.controller.download.resolver.UniProtKBDownloadFieldsParamAndResultProvider;
import org.uniprot.api.uniprotkb.repository.DataStoreTestConfig;

@ContextConfiguration(classes = {DataStoreTestConfig.class, UniProtKBREST.class})
@ActiveProfiles(profiles = "offline")
@AutoConfigureWebClient
@WebMvcTest(UniProtKBController.class)
@ExtendWith(value = {SpringExtension.class})
public class UniProtKBDownloadFieldsIT extends BaseUniprotKBDownloadIT {
    @RegisterExtension
    static UniProtKBDownloadFieldsParamAndResultProvider paramAndResultProvider =
            new UniProtKBDownloadFieldsParamAndResultProvider();

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
    }

    @ParameterizedTest(name = "[{index}]~/download?{0}")
    @MethodSource("paramAndResultByTypeForDefault")
    void testDownloadDefaultFields(DownloadParamAndResult paramAndResult) throws Exception {

        sendAndVerify(paramAndResult, HttpStatus.OK);
    }

    @ParameterizedTest(name = "[{index}]~/download?{0}")
    @MethodSource("paramAndResultByTypeForNonDefault")
    void testDownloadNonDefaultFields(DownloadParamAndResult paramAndResult) throws Exception {

        sendAndVerify(paramAndResult, HttpStatus.OK);
    }

    @ParameterizedTest(name = "[{index}]~/download?{0}")
    @MethodSource("paramAndResultByTypeForInvalid")
    void testDownloadInvalidFields(DownloadParamAndResult paramAndResult) throws Exception {

        sendAndVerify(paramAndResult, HttpStatus.BAD_REQUEST);
    }

    private static Stream<Arguments> paramAndResultByTypeForDefault() {
        return Stream.of(
                Arguments.of(
                        getParamAndResult(MediaType.APPLICATION_JSON, null, MANDATORY_JSON_FIELDS)),
                Arguments.of(
                        getParamAndResult(
                                UniProtMediaType.TSV_MEDIA_TYPE, null, DEFAULT_TSV_FIELDS)),
                Arguments.of(getParamAndResult(UniProtMediaType.FF_MEDIA_TYPE, null, null)),
                Arguments.of(getParamAndResult(MediaType.APPLICATION_XML, null, null)),
                Arguments.of(
                        getParamAndResult(
                                UniProtMediaType.XLS_MEDIA_TYPE, null, DEFAULT_XLS_FIELDS)),
                Arguments.of(getParamAndResult(UniProtMediaType.FASTA_MEDIA_TYPE, null, null)),
                Arguments.of(getParamAndResult(UniProtMediaType.GFF_MEDIA_TYPE, null, null)),
                Arguments.of(getParamAndResult(UniProtMediaType.RDF_MEDIA_TYPE, null, null)));
    }

    private static DownloadParamAndResult getParamAndResult(
            MediaType mediaType, List<String> reqFields, List<String> respFields) {
        return getParamAndResult(
                mediaType, Arrays.asList(BaseUniprotKBDownloadIT.ACC1), reqFields, respFields);
    }

    private static DownloadParamAndResult getParamAndResult(
            MediaType mediaType,
            List<String> accessions,
            List<String> reqFields,
            List<String> respFields) {
        return paramAndResultProvider.getDownloadParamAndResultForFields(
                mediaType, 1, accessions, reqFields, respFields);
    }

    private static Stream<Arguments> paramAndResultByTypeForNonDefault() {
        return Stream.of(
                Arguments.of(
                        getParamAndResult(
                                MediaType.APPLICATION_JSON,
                                REQUESTED_JSON_FIELDS,
                                RETURNED_JSON_FIELDS)),
                Arguments.of(
                        getParamAndResult(
                                UniProtMediaType.TSV_MEDIA_TYPE,
                                REQUESTED_JSON_FIELDS,
                                TSV_RETURNED_HEADERS)),
                Arguments.of(
                        getParamAndResult(
                                UniProtMediaType.XLS_MEDIA_TYPE,
                                REQUESTED_JSON_FIELDS,
                                XLS_RETURNED_HEADERS)),
                Arguments.of(
                        getParamAndResult(
                                UniProtMediaType.FF_MEDIA_TYPE,
                                REQUESTED_JSON_FIELDS,
                                Collections.emptyList())),
                Arguments.of(
                        getParamAndResult(
                                MediaType.APPLICATION_XML,
                                REQUESTED_JSON_FIELDS,
                                Collections.emptyList())),
                Arguments.of(
                        getParamAndResult(
                                UniProtMediaType.FASTA_MEDIA_TYPE,
                                REQUESTED_JSON_FIELDS,
                                Collections.emptyList())),
                Arguments.of(
                        getParamAndResult(
                                UniProtMediaType.GFF_MEDIA_TYPE,
                                REQUESTED_JSON_FIELDS,
                                Collections.emptyList()))
                //
                // Arguments.of(paramAndResultProvider.getDownloadParamAndResultForFields(UniProtMediaType.RDF_MEDIA_TYPE,
                //                        MANDATORY_JSON_FIELDS, Collections.emptyList()))
                );
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
