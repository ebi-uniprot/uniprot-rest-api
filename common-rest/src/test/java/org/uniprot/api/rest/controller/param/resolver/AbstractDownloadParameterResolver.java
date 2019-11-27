package org.uniprot.api.rest.controller.param.resolver;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Collections;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.uniprot.api.rest.controller.param.DownloadParamAndResult;
import org.uniprot.api.rest.output.UniProtMediaType;

public abstract class AbstractDownloadParameterResolver implements ParameterResolver {

    @Override
    public boolean supportsParameter(
            ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return parameterContext.getParameter().getType().equals(DownloadParamAndResult.class);
    }

    @Override
    public Object resolveParameter(
            ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        DownloadParamAndResult result = null;
        Method method =
                extensionContext
                        .getTestMethod()
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "AbstractDownloadParameterResolver: Unable to find test method"));
        switch (method.getName()) {
            case "testDownloadAllJSON":
                result = getDownloadAllParamAndResult(MediaType.APPLICATION_JSON);
                break;
            case "testDownloadAllTSV":
                result = getDownloadAllParamAndResult(UniProtMediaType.TSV_MEDIA_TYPE);
                break;
            case "testDownloadAllList":
                result = getDownloadAllParamAndResult(UniProtMediaType.LIST_MEDIA_TYPE);
                break;
            case "testDownloadAllOBO":
                result = getDownloadAllParamAndResult(UniProtMediaType.OBO_MEDIA_TYPE);
                break;
            case "testDownloadAllXLS":
                result = getDownloadAllParamAndResult(UniProtMediaType.XLS_MEDIA_TYPE);
                break;
            case "testDownloadLessThanDefaultBatchSizeJSON":
                result =
                        getDownloadLessThanDefaultBatchSizeParamAndResult(
                                MediaType.APPLICATION_JSON);
                break;
            case "testDownloadLessThanDefaultBatchSizeTSV":
                result =
                        getDownloadLessThanDefaultBatchSizeParamAndResult(
                                UniProtMediaType.TSV_MEDIA_TYPE);
                break;
            case "testDownloadLessThanDefaultBatchSizeList":
                result =
                        getDownloadLessThanDefaultBatchSizeParamAndResult(
                                UniProtMediaType.LIST_MEDIA_TYPE);
                break;
            case "testDownloadLessThanDefaultBatchSizeOBO":
                result =
                        getDownloadLessThanDefaultBatchSizeParamAndResult(
                                UniProtMediaType.OBO_MEDIA_TYPE);
                break;
            case "testDownloadLessThanDefaultBatchSizeXLS":
                result =
                        getDownloadLessThanDefaultBatchSizeParamAndResult(
                                UniProtMediaType.XLS_MEDIA_TYPE);
                break;
            case "testDownloadDefaultBatchSizeJSON":
                result = getDownloadDefaultBatchSizeParamAndResult(MediaType.APPLICATION_JSON);
                break;
            case "testDownloadMoreThanBatchSizeJSON":
                result = getDownloadMoreThanBatchSizeParamAndResult(MediaType.APPLICATION_JSON);
                break;
            case "testDownloadSizeLessThanZeroJSON":
                result = getDownloadSizeLessThanZeroParamAndResult(MediaType.APPLICATION_JSON);
                break;
            case "testDownloadWithoutQueryJSON":
                result = getDownloadWithoutQueryParamAndResult(MediaType.APPLICATION_JSON);
                break;
            case "testDownloadWithoutQueryTSV":
                result = getDownloadWithoutQueryParamAndResult(UniProtMediaType.TSV_MEDIA_TYPE);
                break;
            case "testDownloadWithoutQueryList":
                result = getDownloadWithoutQueryParamAndResult(UniProtMediaType.LIST_MEDIA_TYPE);
                break;
            case "testDownloadWithoutQueryOBO":
                result = getDownloadWithoutQueryParamAndResult(UniProtMediaType.OBO_MEDIA_TYPE);
                break;
            case "testDownloadWithoutQueryXLS":
                result = getDownloadWithoutQueryParamAndResult(UniProtMediaType.XLS_MEDIA_TYPE);
                break;
            case "testDownloadWithBadQueryJSON":
                result = getDownloadWithBadQueryParamAndResult(MediaType.APPLICATION_JSON);
                break;
            case "testDownloadWithBadQueryList":
                result = getDownloadWithBadQueryParamAndResult(UniProtMediaType.LIST_MEDIA_TYPE);
                break;
            case "testDownloadWithBadQueryTSV":
                result = getDownloadWithBadQueryParamAndResult(UniProtMediaType.TSV_MEDIA_TYPE);
                break;
            case "testDownloadWithBadQueryOBO":
                result = getDownloadWithBadQueryParamAndResult(UniProtMediaType.OBO_MEDIA_TYPE);
                break;
            case "testDownloadWithBadQueryXLS":
                result = getDownloadWithBadQueryParamAndResult(UniProtMediaType.XLS_MEDIA_TYPE);
                break;
            case "testDownloadWithSortJSON":
                result = getDownloadWithSortParamAndResult(MediaType.APPLICATION_JSON);
                break;
            case "testDownloadWithSortList":
                result = getDownloadWithSortParamAndResult(UniProtMediaType.LIST_MEDIA_TYPE);
                break;
            case "testDownloadWithSortTSV":
                result = getDownloadWithSortParamAndResult(UniProtMediaType.TSV_MEDIA_TYPE);
                break;
            case "testDownloadWithSortOBO":
                result = getDownloadWithSortParamAndResult(UniProtMediaType.OBO_MEDIA_TYPE);
                break;
            case "testDownloadWithSortXLS":
                result = getDownloadWithSortParamAndResult(UniProtMediaType.XLS_MEDIA_TYPE);
                break;
        }
        return result;
    }

    protected DownloadParamAndResult getDownloadAllParamAndResult(MediaType contentType) {
        return getCommonDownloadParamAndResult(contentType);
    }

    protected abstract DownloadParamAndResult getDownloadLessThanDefaultBatchSizeParamAndResult(
            MediaType contentType);

    protected abstract DownloadParamAndResult getDownloadDefaultBatchSizeParamAndResult(
            MediaType contentType);

    protected abstract DownloadParamAndResult getDownloadMoreThanBatchSizeParamAndResult(
            MediaType contentType);

    protected abstract DownloadParamAndResult getDownloadSizeLessThanZeroParamAndResult(
            MediaType contentType);

    protected abstract DownloadParamAndResult getDownloadWithSortParamAndResult(
            MediaType applicationJson);

    // negative test cases
    protected abstract DownloadParamAndResult getDownloadWithoutQueryParamAndResult(
            MediaType contentType);

    protected abstract DownloadParamAndResult getDownloadWithBadQueryParamAndResult(
            MediaType contentType);

    protected abstract void verifyExcelData(Sheet sheet);

    private DownloadParamAndResult getCommonDownloadParamAndResult(MediaType contentType) {
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

    protected Integer getExcelRowCountAndVerifyContent(MvcResult result) throws IOException {
        byte[] xlsBin = result.getResponse().getContentAsByteArray();
        InputStream excelFile = new ByteArrayInputStream(xlsBin);
        Workbook workbook = new XSSFWorkbook(excelFile);
        Sheet sheet = workbook.getSheetAt(0);
        // specific to the data type e.g. disease, cross ref etc
        verifyExcelData(sheet);
        return sheet.getPhysicalNumberOfRows();
    }
}
