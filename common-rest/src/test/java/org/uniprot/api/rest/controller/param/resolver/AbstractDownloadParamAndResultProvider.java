package org.uniprot.api.rest.controller.param.resolver;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.uniprot.api.rest.controller.param.DownloadParamAndResult;
import org.uniprot.api.rest.output.UniProtMediaType;

/** class to provide common query param and result matcher */
public abstract class AbstractDownloadParamAndResultProvider {
    public DownloadParamAndResult getDownloadParamAndResult(
            MediaType contentType, Integer entryCount) {
        DownloadParamAndResult.DownloadParamAndResultBuilder builder =
                DownloadParamAndResult.builder();
        builder.queryParam("query", Collections.singletonList("*")).contentType(contentType);

        List<ResultMatcher> resultMatchers =
                getResultMatchers(contentType, entryCount, null, null, null, null, null);
        builder.resultMatchers(resultMatchers);
        return builder.build();
    }

    public DownloadParamAndResult getDownloadParamAndResultForSort(
            MediaType contentType,
            String sortFieldName,
            String sortOrder,
            Integer entryCount,
            List<String> accessionsInOrder) {

        DownloadParamAndResult.DownloadParamAndResultBuilder builder =
                DownloadParamAndResult.builder();
        builder.queryParam("query", Collections.singletonList("*")).contentType(contentType);
        List<ResultMatcher> resultMatchers =
                getResultMatchers(
                        contentType,
                        entryCount,
                        sortFieldName,
                        sortOrder,
                        accessionsInOrder,
                        null,
                        null);
        builder.resultMatchers(resultMatchers);
        DownloadParamAndResult paramAndResult = builder.build();

        // add sort param
        Map<String, List<String>> updatedQueryParams =
                addQueryParam(
                        paramAndResult.getQueryParams(),
                        "sort",
                        Collections.singletonList(sortFieldName + " " + sortOrder));

        paramAndResult.setQueryParams(updatedQueryParams);

        return paramAndResult;
    }

    public DownloadParamAndResult getDownloadParamAndResultForFields(
            MediaType contentType,
            Integer entryCount,
            List<String> accessionsInOrder,
            List<String> requestedFields,
            List<String> expectedFields) {
        // default query
        DownloadParamAndResult.DownloadParamAndResultBuilder builder =
                DownloadParamAndResult.builder();
        builder.queryParam("query", Collections.singletonList("*")).contentType(contentType);

        List<ResultMatcher> resultMatchers =
                getResultMatchers(
                        contentType,
                        entryCount,
                        null,
                        null,
                        accessionsInOrder,
                        requestedFields,
                        expectedFields);
        builder.resultMatchers(resultMatchers);
        DownloadParamAndResult paramAndResult = builder.build();

        if (Objects.nonNull(requestedFields) && !requestedFields.isEmpty()) {
            String commaSeparated = requestedFields.stream().collect(Collectors.joining(","));
            // add fields param
            Map<String, List<String>> updatedQueryParams =
                    addQueryParam(
                            paramAndResult.getQueryParams(),
                            "fields",
                            Collections.singletonList(commaSeparated));

            paramAndResult.setQueryParams(updatedQueryParams);
        }

        return paramAndResult;
    }

    protected List<ResultMatcher> getResultMatchers(
            MediaType contentType,
            Integer entryCount,
            String sortFieldName,
            String sortOrder,
            List<String> accessionsInOrder,
            List<String> requestedFields,
            List<String> expectedFields) {

        List<ResultMatcher> resultMatchers;
        if (MediaType.APPLICATION_JSON.equals(contentType)) {
            resultMatchers =
                    getJsonResultMatchers(
                            entryCount,
                            sortFieldName,
                            sortOrder,
                            accessionsInOrder,
                            requestedFields,
                            expectedFields);
        } else if (UniProtMediaType.TSV_MEDIA_TYPE.equals(contentType)) {
            resultMatchers =
                    getTSVResultMatchers(
                            entryCount,
                            sortFieldName,
                            sortOrder,
                            accessionsInOrder,
                            requestedFields,
                            expectedFields);
        } else if (UniProtMediaType.LIST_MEDIA_TYPE.equals(contentType)) {
            resultMatchers =
                    getListResultMatchers(
                            entryCount,
                            sortFieldName,
                            sortOrder,
                            accessionsInOrder,
                            requestedFields,
                            expectedFields);
        } else if (UniProtMediaType.XLS_MEDIA_TYPE.equals(contentType)) {
            resultMatchers =
                    getXLSResultMatchers(
                            entryCount,
                            sortFieldName,
                            sortOrder,
                            accessionsInOrder,
                            requestedFields,
                            expectedFields);
        } else if (UniProtMediaType.RDF_MEDIA_TYPE.equals(contentType)) {
            resultMatchers =
                    getRDFResultMatchers(
                            entryCount,
                            sortFieldName,
                            sortOrder,
                            accessionsInOrder,
                            requestedFields,
                            expectedFields);
        } else if (UniProtMediaType.FF_MEDIA_TYPE.equals(contentType)) {
            resultMatchers =
                    getFFResultMatchers(
                            entryCount,
                            sortFieldName,
                            sortOrder,
                            accessionsInOrder,
                            requestedFields,
                            expectedFields);
        } else if (MediaType.APPLICATION_XML.equals(contentType)) {
            resultMatchers =
                    getXMLResultMatchers(
                            entryCount,
                            sortFieldName,
                            sortOrder,
                            accessionsInOrder,
                            requestedFields,
                            expectedFields);
        } else if (UniProtMediaType.FASTA_MEDIA_TYPE.equals(contentType)) {
            resultMatchers =
                    getFastaResultMatchers(
                            entryCount,
                            sortFieldName,
                            sortOrder,
                            accessionsInOrder,
                            requestedFields,
                            expectedFields);
        } else if (UniProtMediaType.GFF_MEDIA_TYPE.equals(contentType)) {
            resultMatchers =
                    getGFFResultMatchers(
                            entryCount,
                            sortFieldName,
                            sortOrder,
                            accessionsInOrder,
                            requestedFields,
                            expectedFields);
        } else if (UniProtMediaType.OBO_MEDIA_TYPE.equals(contentType)) {
            resultMatchers =
                    getOBOResultMatchers(
                            entryCount,
                            sortFieldName,
                            sortOrder,
                            accessionsInOrder,
                            requestedFields,
                            expectedFields);
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

    protected abstract List<ResultMatcher> getGFFResultMatchers(
            Integer entryCount,
            String sortFieldName,
            String sortOrder,
            List<String> accessionsInOrder,
            List<String> requestedFields,
            List<String> expectedFields);

    protected abstract List<ResultMatcher> getFastaResultMatchers(
            Integer entryCount,
            String sortFieldName,
            String sortOrder,
            List<String> accessionsInOrder,
            List<String> requestedFields,
            List<String> expectedFields);

    protected abstract List<ResultMatcher> getXMLResultMatchers(
            Integer entryCount,
            String sortFieldName,
            String sortOrder,
            List<String> accessionsInOrder,
            List<String> requestedFields,
            List<String> expectedFields);

    protected abstract List<ResultMatcher> getOBOResultMatchers(
            Integer entryCount,
            String sortFieldName,
            String sortOrder,
            List<String> accessionsInOrder,
            List<String> requestedFields,
            List<String> expectedFields);

    protected abstract List<ResultMatcher> getFFResultMatchers(
            Integer entryCount,
            String sortFieldName,
            String sortOrder,
            List<String> accessionsInOrder,
            List<String> requestedFields,
            List<String> expectedFields);

    protected abstract List<ResultMatcher> getRDFResultMatchers(
            Integer entryCount,
            String sortFieldName,
            String sortOrder,
            List<String> accessionsInOrder,
            List<String> requestedFields,
            List<String> expectedFields);

    protected abstract List<ResultMatcher> getXLSResultMatchers(
            Integer entryCount,
            String sortFieldName,
            String sortOrder,
            List<String> accessionsInOrder,
            List<String> requestedFields,
            List<String> expectedFields);

    protected abstract List<ResultMatcher> getListResultMatchers(
            Integer entryCount,
            String sortFieldName,
            String sortOrder,
            List<String> accessionsInOrder,
            List<String> requestedFields,
            List<String> expectedFields);

    protected abstract List<ResultMatcher> getTSVResultMatchers(
            Integer entryCount,
            String sortFieldName,
            String sortOrder,
            List<String> accessionsInOrder,
            List<String> requestedFields,
            List<String> expectedFields);

    protected abstract List<ResultMatcher> getJsonResultMatchers(
            Integer entryCount,
            String sortFieldName,
            String sortOrder,
            List<String> accessionsInOrder,
            List<String> requestedFields,
            List<String> expectedFields);
}
