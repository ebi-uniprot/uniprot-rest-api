package org.uniprot.api.rest.controller.param.resolver;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.uniprot.api.rest.controller.param.DownloadParamAndResult;
import org.uniprot.api.rest.output.UniProtMediaType;

public abstract class AbstractDownloadParamAndResultProvider {

    public DownloadParamAndResult getDownloadParamAndResult(
            MediaType contentType, Integer entryCount) {
        DownloadParamAndResult.DownloadParamAndResultBuilder builder =
                DownloadParamAndResult.builder();
        builder.queryParam("query", Collections.singletonList("*")).contentType(contentType);

        List<ResultMatcher> resultMatchers = getResultMatchers(contentType, entryCount);
        builder.resultMatchers(resultMatchers);
        return builder.build();
    }

    protected List<ResultMatcher> getResultMatchers(
            MediaType contentType, Integer expectedEntryCount) {
        List<ResultMatcher> resultMatchers;
        if (MediaType.APPLICATION_JSON.equals(contentType)) {
            resultMatchers = getJsonResultMatchers(expectedEntryCount);
        } else if (UniProtMediaType.TSV_MEDIA_TYPE.equals(contentType)) {
            resultMatchers = getTSVResultMatchers(expectedEntryCount);
        } else if (UniProtMediaType.LIST_MEDIA_TYPE.equals(contentType)) {
            resultMatchers = getListResultMatchers(expectedEntryCount);
        } else if (UniProtMediaType.XLS_MEDIA_TYPE.equals(contentType)) {
            resultMatchers = getXLSResultMatchers(expectedEntryCount);
        } else if (UniProtMediaType.RDF_MEDIA_TYPE.equals(contentType)) {
            resultMatchers = getRDFResultMatchers(expectedEntryCount);
        } else if (UniProtMediaType.FF_MEDIA_TYPE.equals(contentType)) {
            resultMatchers = getFFResultMatchers(expectedEntryCount);
        } else if (MediaType.APPLICATION_XML.equals(contentType)) {
            resultMatchers = getXMLResultMatchers(expectedEntryCount);
        } else if (UniProtMediaType.FASTA_MEDIA_TYPE.equals(contentType)) {
            resultMatchers = getFastaResultMatchers(expectedEntryCount);
        } else if (UniProtMediaType.GFF_MEDIA_TYPE.equals(contentType)) {
            resultMatchers = getGFFResultMatchers(expectedEntryCount);
        } else if (UniProtMediaType.OBO_MEDIA_TYPE.equals(contentType)) {
            resultMatchers = getOBOResultMatchers(expectedEntryCount);
        } else {
            throw new IllegalArgumentException("Unknown content type " + contentType);
        }
        return resultMatchers;
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

    public Map<String, List<String>> addQueryParam(
            Map<String, List<String>> queryParams, String paramName, List<String> values) {
        Map<String, List<String>> updatedQueryParams = new HashMap<>(queryParams);
        updatedQueryParams.put(paramName, values);
        return updatedQueryParams;
    }

    protected abstract void verifyExcelData(Sheet sheet);

    protected abstract List<ResultMatcher> getGFFResultMatchers(Integer expectedEntryCount);

    protected abstract List<ResultMatcher> getFastaResultMatchers(Integer expectedEntryCount);

    protected abstract List<ResultMatcher> getXMLResultMatchers(Integer expectedEntryCount);

    protected abstract List<ResultMatcher> getOBOResultMatchers(Integer expectedEntryCount);

    protected abstract List<ResultMatcher> getFFResultMatchers(Integer expectedEntryCount);

    protected abstract List<ResultMatcher> getRDFResultMatchers(Integer expectedEntryCount);

    protected abstract List<ResultMatcher> getXLSResultMatchers(Integer expectedEntryCount);

    protected abstract List<ResultMatcher> getListResultMatchers(Integer expectedEntryCount);

    protected abstract List<ResultMatcher> getTSVResultMatchers(Integer expectedEntryCount);

    protected abstract List<ResultMatcher> getJsonResultMatchers(Integer expectedEntryCount);
}
