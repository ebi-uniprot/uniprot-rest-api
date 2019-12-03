package org.uniprot.api.rest.controller.param.resolver;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.uniprot.api.rest.controller.param.DownloadParamAndResult;
import org.uniprot.api.rest.output.UniProtMediaType;

/** Class to keep common code used by all type download resolver */
public abstract class BaseDownloadParamResolver {

    protected abstract void verifyExcelData(Sheet sheet);

    protected DownloadParamAndResult getCommonDownloadParamAndResult(MediaType contentType) {
        DownloadParamAndResult.DownloadParamAndResultBuilder builder =
                DownloadParamAndResult.builder()
                        .queryParam("query", Collections.singletonList("*"))
                        .contentType(contentType);
        if (UniProtMediaType.OBO_MEDIA_TYPE.equals(contentType)) {
            addOBOResultMatcher(builder);
        } else if (UniProtMediaType.XLS_MEDIA_TYPE.equals(contentType)) {
            addXLSResultMatcher(builder);
        }
        return builder.build();
    }

    protected Integer getExcelRowCountAndVerifyContent(MvcResult result) throws IOException {
        byte[] xlsBin = result.getResponse().getContentAsByteArray();
        InputStream excelFile = new ByteArrayInputStream(xlsBin);
        Workbook workbook = new XSSFWorkbook(excelFile);
        Sheet sheet = workbook.getSheetAt(0);
        // specific to the data type e.g. disease, cross ref etc
        verifyExcelData(sheet);
        return sheet.getPhysicalNumberOfRows();
    }

    private void addXLSResultMatcher(DownloadParamAndResult.DownloadParamAndResultBuilder builder) {
        builder.resultMatcher(
                result ->
                        assertThat(
                                "The excel response is empty",
                                result.getResponse().getContentAsString(),
                                not(isEmptyOrNullString())));
    }

    private void addOBOResultMatcher(DownloadParamAndResult.DownloadParamAndResultBuilder builder) {
        builder.resultMatcher(
                result ->
                        assertThat(
                                "The obo response doesn't start with correct format",
                                result.getResponse()
                                        .getContentAsString()
                                        .startsWith("format-version: 1.2")));
    }
}
