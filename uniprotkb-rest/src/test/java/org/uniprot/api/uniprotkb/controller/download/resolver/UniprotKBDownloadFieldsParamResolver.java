package org.uniprot.api.uniprotkb.controller.download.resolver;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.MediaType;
import org.uniprot.api.rest.controller.param.DownloadParamAndResult;
import org.uniprot.api.rest.controller.param.resolver.AbstractDownloadFieldsResolver;
import org.uniprot.api.uniprotkb.controller.download.IT.BaseUniprotKBDownloadIT;

public class UniprotKBDownloadFieldsParamResolver extends AbstractDownloadFieldsResolver {

    @RegisterExtension
    static UniProtKBDownloadFieldsParamAndResultProvider paramAndResultProvider =
            new UniProtKBDownloadFieldsParamAndResultProvider();

    @Override
    public DownloadParamAndResult getDownloadDefaultFieldsParamAndResult(
            MediaType contentType, List<String> expectedFields) {
        // set mandatory fields for json response type
        expectedFields =
                MediaType.APPLICATION_JSON.equals(contentType)
                        ? BaseUniprotKBDownloadIT.MANDATORY_JSON_FIELDS
                        : expectedFields;

        DownloadParamAndResult paramAndResult =
                paramAndResultProvider.getDownloadParamAndResultForFields(
                        contentType,
                        1,
                        Arrays.asList(BaseUniprotKBDownloadIT.ACC1),
                        null,
                        expectedFields);
        return paramAndResult;
    }

    @Override
    public DownloadParamAndResult getDownloadNonDefaultFieldsParamAndResult(
            MediaType contentType, List<String> requestedFields, List<String> expectedFields) {
        expectedFields =
                MediaType.APPLICATION_JSON.equals(contentType)
                        ? BaseUniprotKBDownloadIT.RETURNED_JSON_FIELDS
                        : expectedFields;
        requestedFields =
                MediaType.APPLICATION_JSON.equals(contentType)
                        ? BaseUniprotKBDownloadIT.REQUESTED_JSON_FIELDS
                        : requestedFields;
        DownloadParamAndResult paramAndResult =
                paramAndResultProvider.getDownloadParamAndResultForFields(
                        contentType,
                        1,
                        Arrays.asList(BaseUniprotKBDownloadIT.ACC1),
                        requestedFields,
                        expectedFields);
        return paramAndResult;
    }

    @Override
    public DownloadParamAndResult getDownloadInvalidFieldsParamAndResult(
            MediaType contentType, List<String> invalidRequestedFields) {
        invalidRequestedFields =
                MediaType.APPLICATION_JSON.equals(contentType)
                        ? BaseUniprotKBDownloadIT.INVALID_RETURN_FIELDS
                        : invalidRequestedFields;
        return paramAndResultProvider.getDownloadParamAndResultForFields(
                contentType, 1, null, invalidRequestedFields, Collections.emptyList());
    }

    @Override
    protected void verifyExcelData(Sheet sheet) {}
}
