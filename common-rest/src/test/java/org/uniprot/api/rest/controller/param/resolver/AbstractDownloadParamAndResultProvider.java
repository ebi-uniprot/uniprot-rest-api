package org.uniprot.api.rest.controller.param.resolver;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.extension.Extension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.uniprot.api.rest.controller.param.DownloadParamAndResult;
import org.uniprot.api.rest.output.UniProtMediaType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/** class to provide common query param and result matcher */
public abstract class AbstractDownloadParamAndResultProvider implements Extension {
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
        switch (contentType.toString()) {
            case MediaType.APPLICATION_JSON_VALUE:
                resultMatchers =
                        getJsonResultMatchers(
                                entryCount,
                                sortFieldName,
                                sortOrder,
                                accessionsInOrder,
                                requestedFields,
                                expectedFields);
                break;
            case UniProtMediaType.TSV_MEDIA_TYPE_VALUE:
                resultMatchers =
                        getTsvResultMatchers(
                                entryCount,
                                sortFieldName,
                                sortOrder,
                                accessionsInOrder,
                                requestedFields,
                                expectedFields);
                break;
            case UniProtMediaType.LIST_MEDIA_TYPE_VALUE:
                resultMatchers =
                        getListResultMatchers(
                                entryCount,
                                sortFieldName,
                                sortOrder,
                                accessionsInOrder,
                                requestedFields,
                                expectedFields);
                break;
            case UniProtMediaType.XLS_MEDIA_TYPE_VALUE:
                resultMatchers =
                        getXlsResultMatchers(
                                entryCount,
                                sortFieldName,
                                sortOrder,
                                accessionsInOrder,
                                requestedFields,
                                expectedFields);
                break;
            case UniProtMediaType.RDF_MEDIA_TYPE_VALUE:
                resultMatchers =
                        getRdfResultMatchers(
                                entryCount,
                                sortFieldName,
                                sortOrder,
                                accessionsInOrder,
                                requestedFields,
                                expectedFields);
                break;
            case UniProtMediaType.FF_MEDIA_TYPE_VALUE:
                resultMatchers =
                        getFfResultMatchers(
                                entryCount,
                                sortFieldName,
                                sortOrder,
                                accessionsInOrder,
                                requestedFields,
                                expectedFields);
                break;
            case MediaType.APPLICATION_XML_VALUE:
                resultMatchers =
                        getXmlResultMatchers(
                                entryCount,
                                sortFieldName,
                                sortOrder,
                                accessionsInOrder,
                                requestedFields,
                                expectedFields);
                break;
            case UniProtMediaType.FASTA_MEDIA_TYPE_VALUE:
                resultMatchers =
                        getFastaResultMatchers(
                                entryCount,
                                sortFieldName,
                                sortOrder,
                                accessionsInOrder,
                                requestedFields,
                                expectedFields);
                break;
            case UniProtMediaType.GFF_MEDIA_TYPE_VALUE:
                resultMatchers =
                        getGFFResultMatchers(
                                entryCount,
                                sortFieldName,
                                sortOrder,
                                accessionsInOrder,
                                requestedFields,
                                expectedFields);
                break;
            case UniProtMediaType.OBO_MEDIA_TYPE_VALUE:
                resultMatchers =
                        getOboResultMatchers(
                                entryCount,
                                sortFieldName,
                                sortOrder,
                                accessionsInOrder,
                                requestedFields,
                                expectedFields);
                break;
            default:
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

    protected abstract List<ResultMatcher> getXmlResultMatchers(
            Integer entryCount,
            String sortFieldName,
            String sortOrder,
            List<String> accessionsInOrder,
            List<String> requestedFields,
            List<String> expectedFields);

    protected abstract List<ResultMatcher> getOboResultMatchers(
            Integer entryCount,
            String sortFieldName,
            String sortOrder,
            List<String> accessionsInOrder,
            List<String> requestedFields,
            List<String> expectedFields);

    protected abstract List<ResultMatcher> getFfResultMatchers(
            Integer entryCount,
            String sortFieldName,
            String sortOrder,
            List<String> accessionsInOrder,
            List<String> requestedFields,
            List<String> expectedFields);

    protected abstract List<ResultMatcher> getRdfResultMatchers(
            Integer entryCount,
            String sortFieldName,
            String sortOrder,
            List<String> accessionsInOrder,
            List<String> requestedFields,
            List<String> expectedFields);

    protected abstract List<ResultMatcher> getXlsResultMatchers(
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

    protected abstract List<ResultMatcher> getTsvResultMatchers(
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
