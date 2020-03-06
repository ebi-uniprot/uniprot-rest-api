package org.uniprot.api.uniprotkb.controller.download.IT;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.rest.controller.param.DownloadParamAndResult;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.uniprotkb.UniProtKBREST;
import org.uniprot.api.uniprotkb.controller.UniprotKBController;
import org.uniprot.api.uniprotkb.controller.download.resolver.UniprotKBDownloadFieldsParamResolver;
import org.uniprot.api.uniprotkb.repository.DataStoreTestConfig;

import java.util.Collections;
import java.util.stream.Stream;

@ContextConfiguration(classes = {DataStoreTestConfig.class, UniProtKBREST.class})
@ActiveProfiles(profiles = "offline")
@AutoConfigureWebClient
@WebMvcTest(UniprotKBController.class)
@ExtendWith(value = {SpringExtension.class})
public class UniProtKBDownloadFieldsIT extends BaseUniprotKBDownloadIT {
    @RegisterExtension
    static UniprotKBDownloadFieldsParamResolver paramResolver =
            new UniprotKBDownloadFieldsParamResolver();

    @BeforeEach
    public void setUpData() {
        // when
        saveEntry(ACC1, 1);
    }

    @Test
    protected void testDownloadDefaultFieldsJSON(DownloadParamAndResult paramAndResult)
            throws Exception {
        sendAndVerify(paramAndResult, HttpStatus.OK);
    }

    @Test
    protected void testDownloadNonDefaultFieldsJSON(DownloadParamAndResult paramAndResult)
            throws Exception {
        sendAndVerify(paramAndResult, HttpStatus.OK);
    }

    @Test
    protected void testDownloadInvalidFieldsJSON(DownloadParamAndResult paramAndResult)
            throws Exception {
        sendAndVerify(paramAndResult, HttpStatus.BAD_REQUEST);
    }

    @ParameterizedTest(name = "[{index}]~/download?{0}")
    @MethodSource("paramAndResultByTypeForDefault")
    void testDownloadDefaultFields(DownloadParamAndResult paramAndResult) throws Exception {

        sendAndVerify(paramAndResult, HttpStatus.OK);
    }

    @Disabled
    @ParameterizedTest(name = "[{index}]~/download?{0}")
    @MethodSource("paramAndResultByTypeForNonDefault")
    void testDownloadNonDefaultFields(DownloadParamAndResult paramAndResult) throws Exception {

        sendAndVerify(paramAndResult, HttpStatus.OK);
    }

    @Disabled
    @ParameterizedTest(name = "[{index}]~/download?{0}")
    @MethodSource("paramAndResultByTypeForInvalid")
    void testDownloadInvalidFields(DownloadParamAndResult paramAndResult) throws Exception {

        sendAndVerify(paramAndResult, HttpStatus.OK);
    }

    private static Stream<Arguments> paramAndResultByTypeForDefault(){
        return Stream.of(Arguments.of(paramResolver.getDownloadDefaultFieldsParamAndResult(UniProtMediaType.TSV_MEDIA_TYPE, TSV_DEFAULT_FIELDS)),
        Arguments.of(paramResolver.getDownloadDefaultFieldsParamAndResult(UniProtMediaType.FF_MEDIA_TYPE, MANDATORY_JSON_FIELDS)),
        Arguments.of(paramResolver.getDownloadDefaultFieldsParamAndResult(MediaType.APPLICATION_XML, MANDATORY_JSON_FIELDS)),
        Arguments.of(paramResolver.getDownloadDefaultFieldsParamAndResult(UniProtMediaType.XLS_MEDIA_TYPE, MANDATORY_JSON_FIELDS)),
        Arguments.of(paramResolver.getDownloadDefaultFieldsParamAndResult(UniProtMediaType.FASTA_MEDIA_TYPE, MANDATORY_JSON_FIELDS)),
        Arguments.of(paramResolver.getDownloadDefaultFieldsParamAndResult(UniProtMediaType.GFF_MEDIA_TYPE, MANDATORY_JSON_FIELDS))//,
        //Arguments.of(paramResolver.getDownloadDefaultFieldsParamAndResult(UniProtMediaType.RDF_MEDIA_TYPE, MANDATORY_JSON_FIELDS))
                );
    }

    private static Stream<Arguments> paramAndResultByTypeForNonDefault(){
        return Stream.of(Arguments.of(paramResolver.getDownloadNonDefaultFieldsParamAndResult(UniProtMediaType.TSV_MEDIA_TYPE, REQUESTED_JSON_FIELDS, Collections.emptyList())),
                Arguments.of(paramResolver.getDownloadNonDefaultFieldsParamAndResult(UniProtMediaType.FF_MEDIA_TYPE, REQUESTED_JSON_FIELDS, Collections.emptyList())),
                Arguments.of(paramResolver.getDownloadNonDefaultFieldsParamAndResult(MediaType.APPLICATION_XML, REQUESTED_JSON_FIELDS, Collections.emptyList())),
                Arguments.of(paramResolver.getDownloadNonDefaultFieldsParamAndResult(UniProtMediaType.XLS_MEDIA_TYPE, REQUESTED_JSON_FIELDS, Collections.emptyList())),
                Arguments.of(paramResolver.getDownloadNonDefaultFieldsParamAndResult(UniProtMediaType.FASTA_MEDIA_TYPE, REQUESTED_JSON_FIELDS, Collections.emptyList())),
                Arguments.of(paramResolver.getDownloadNonDefaultFieldsParamAndResult(UniProtMediaType.GFF_MEDIA_TYPE, REQUESTED_JSON_FIELDS, Collections.emptyList())),
                Arguments.of(paramResolver.getDownloadNonDefaultFieldsParamAndResult(UniProtMediaType.RDF_MEDIA_TYPE, REQUESTED_JSON_FIELDS, Collections.emptyList())));
    }

    private static Stream<Arguments> paramAndResultByTypeForInvalid(){
        return Stream.of(Arguments.of(paramResolver.getDownloadInvalidFieldsParamAndResult(UniProtMediaType.TSV_MEDIA_TYPE, INVALID_RETURN_FIELDS)),
                Arguments.of(paramResolver.getDownloadInvalidFieldsParamAndResult(UniProtMediaType.FF_MEDIA_TYPE, INVALID_RETURN_FIELDS)),
                Arguments.of(paramResolver.getDownloadInvalidFieldsParamAndResult(MediaType.APPLICATION_XML, INVALID_RETURN_FIELDS)),
                Arguments.of(paramResolver.getDownloadInvalidFieldsParamAndResult(UniProtMediaType.XLS_MEDIA_TYPE, INVALID_RETURN_FIELDS)),
                Arguments.of(paramResolver.getDownloadInvalidFieldsParamAndResult(UniProtMediaType.FASTA_MEDIA_TYPE, INVALID_RETURN_FIELDS)),
                Arguments.of(paramResolver.getDownloadInvalidFieldsParamAndResult(UniProtMediaType.GFF_MEDIA_TYPE, INVALID_RETURN_FIELDS)),
                Arguments.of(paramResolver.getDownloadInvalidFieldsParamAndResult(UniProtMediaType.RDF_MEDIA_TYPE, INVALID_RETURN_FIELDS)));
    }
}
