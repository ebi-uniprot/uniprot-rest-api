package org.uniprot.api.uniprotkb.controller.download.IT;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
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
import org.uniprot.api.uniprotkb.controller.UniprotKBController;
import org.uniprot.api.uniprotkb.controller.download.resolver.UniprotKBDownloadAllParamResolver;
import org.uniprot.api.uniprotkb.repository.DataStoreTestConfig;

/**
 * Class to test download api without any filter.. kind of download everything for each type of
 * content type
 */
@ContextConfiguration(classes = {DataStoreTestConfig.class, UniProtKBREST.class})
@ActiveProfiles(profiles = "offline")
@AutoConfigureWebClient
@WebMvcTest(UniprotKBController.class)
@ExtendWith(value = {SpringExtension.class})
public class UniProtKBDownloadAllIT extends BaseUniprotKBDownloadIT {

    @RegisterExtension
    static UniprotKBDownloadAllParamResolver paramResolver =
            new UniprotKBDownloadAllParamResolver();

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

    @Test
    protected void testDownloadAllJSON(DownloadParamAndResult paramAndResult) throws Exception {
        sendAndVerify(paramAndResult, HttpStatus.OK);
    }

    @ParameterizedTest(name = "[{index}]~/download?{0}")
    @MethodSource("provideRequestResponseByType")
    void testDownloadAll(DownloadParamAndResult paramAndResult) throws Exception {
        sendAndVerify(paramAndResult, HttpStatus.OK);
    }

    private static Stream<Arguments> provideRequestResponseByType() {
        return Stream.of(
                Arguments.of(
                        paramResolver.getDownloadAllParamAndResult(
                                UniProtMediaType.TSV_MEDIA_TYPE)),
                Arguments.of(
                        paramResolver.getDownloadAllParamAndResult(UniProtMediaType.FF_MEDIA_TYPE)),
                Arguments.of(
                        paramResolver.getDownloadAllParamAndResult(
                                UniProtMediaType.LIST_MEDIA_TYPE)),
                Arguments.of(paramResolver.getDownloadAllParamAndResult(MediaType.APPLICATION_XML)),
                Arguments.of(
                        paramResolver.getDownloadAllParamAndResult(
                                UniProtMediaType.XLS_MEDIA_TYPE)),
                Arguments.of(
                        paramResolver.getDownloadAllParamAndResult(
                                UniProtMediaType.FASTA_MEDIA_TYPE)),
                Arguments.of(
                        paramResolver.getDownloadAllParamAndResult(
                                UniProtMediaType.GFF_MEDIA_TYPE)),
                Arguments.of(
                        paramResolver.getDownloadAllParamAndResult(
                                UniProtMediaType.RDF_MEDIA_TYPE)));
    }
}
