package org.uniprot.api.disease.download;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.uniprot.api.rest.controller.AbstractDownloadControllerIT.ENTRY_COUNT;

import java.util.*;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;
import org.uniprot.api.rest.controller.param.DownloadParamAndResult;
import org.uniprot.api.rest.controller.param.resolver.AbstractDownloadParameterResolver;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.service.BasicSearchService;

public class DiseaseDownloadParameterResolver extends AbstractDownloadParameterResolver {

    @Override
    protected DownloadParamAndResult getDownloadAllParamAndResult(MediaType contentType) {
        return getDownloadDefaultParamAndResult(contentType, ENTRY_COUNT);
    }

    @Override
    protected DownloadParamAndResult getDownloadLessThanDefaultBatchSizeParamAndResult(
            MediaType contentType) {
        Integer downloadSize = BasicSearchService.DEFAULT_SOLR_BATCH_SIZE - 40;
        DownloadParamAndResult paramAndResult =
                getDownloadDefaultParamAndResult(contentType, downloadSize);
        // add param
        Map<String, List<String>> updatedQueryParams =
                addQueryParam(
                        paramAndResult.getQueryParams(),
                        "size",
                        Collections.singletonList(String.valueOf(downloadSize)));
        paramAndResult.setQueryParams(updatedQueryParams);
        return paramAndResult;
    }

    @Override
    protected DownloadParamAndResult getDownloadDefaultBatchSizeParamAndResult(
            MediaType contentType) {
        Integer downloadSize = BasicSearchService.DEFAULT_SOLR_BATCH_SIZE;
        DownloadParamAndResult paramAndResult =
                getDownloadDefaultParamAndResult(contentType, downloadSize);
        // add param
        Map<String, List<String>> updatedQueryParams =
                addQueryParam(
                        paramAndResult.getQueryParams(),
                        "size",
                        Collections.singletonList(String.valueOf(downloadSize)));
        paramAndResult.setQueryParams(updatedQueryParams);
        return paramAndResult;
    }

    @Override
    protected DownloadParamAndResult getDownloadMoreThanBatchSizeParamAndResult(
            MediaType contentType) {
        Integer downloadSize = BasicSearchService.DEFAULT_SOLR_BATCH_SIZE * 3;
        DownloadParamAndResult paramAndResult =
                getDownloadDefaultParamAndResult(contentType, downloadSize);
        // add param
        Map<String, List<String>> updatedQueryParams =
                addQueryParam(
                        paramAndResult.getQueryParams(),
                        "size",
                        Collections.singletonList(String.valueOf(downloadSize)));
        paramAndResult.setQueryParams(updatedQueryParams);
        return paramAndResult;
    }

    @Override
    protected DownloadParamAndResult getDownloadSizeLessThanZeroParamAndResult(
            MediaType contentType) {
        return DownloadParamAndResult.builder()
                .queryParam("query", Collections.singletonList("*"))
                .queryParam("size", Collections.singletonList(String.valueOf(-1)))
                .contentType(contentType)
                .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                .resultMatcher(jsonPath("$.messages.*", contains("'size' must be greater than 0")))
                .build();
    }

    @Override
    protected DownloadParamAndResult getDownloadWithoutQueryParamAndResult(MediaType contentType) {

        DownloadParamAndResult.DownloadParamAndResultBuilder builder =
                DownloadParamAndResult.builder().contentType(contentType);

        if (MediaType.APPLICATION_JSON.equals(contentType)) {
            builder.resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(
                            jsonPath("$.messages.*", contains("'query' is a required parameter")));
        }

        return builder.build();
    }

    @Override
    protected DownloadParamAndResult getDownloadWithBadQueryParamAndResult(MediaType contentType) {
        return DownloadParamAndResult.builder()
                .queryParam("query", Collections.singletonList("random_field:protein"))
                .contentType(contentType)
                .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                .resultMatcher(
                        jsonPath(
                                "$.messages.*",
                                contains("'random_field' is not a valid search field")))
                .build();
    }

    @Override
    protected void verifyExcelData(Sheet sheet) {
        List<String> headerList = new ArrayList<>();
        boolean headerRead = false;
        for (Row row : sheet) {
            int i = 0;
            for (Cell cell : row) {
                if (!headerRead) {
                    headerList.add(cell.getStringCellValue());
                } else {
                    assertThat(
                            headerList.get(i) + " is null", !cell.getStringCellValue().isEmpty());
                    i++;
                }
            }
            headerRead = true;
        }
        assertThat(
                "Header of excel file doesn't match",
                headerList.equals(Arrays.asList("Name", "Disease ID", "Mnemonic", "Description")));
    }

    private DownloadParamAndResult getDownloadDefaultParamAndResult(
            MediaType contentType, Integer entryCount) {
        // add the common param and result matcher
        DownloadParamAndResult paramAndResult = super.getDownloadAllParamAndResult(contentType);
        // add disease specific result matcher
        List<ResultMatcher> resultMatchers =
                getDiseaseSpecificResultMatcher(
                        contentType, paramAndResult.getResultMatchers(), entryCount);
        paramAndResult.setResultMatchers(resultMatchers);
        return paramAndResult;
    }

    private List<ResultMatcher> getDiseaseSpecificResultMatcher(
            MediaType contentType,
            List<ResultMatcher> oldResultMatchers,
            Integer expectedEntryCount) {
        List<ResultMatcher> resultMatchers = new ArrayList<>(oldResultMatchers);
        if (MediaType.APPLICATION_JSON.equals(contentType)) {
            resultMatchers.add(jsonPath("$.results.length()", is(expectedEntryCount)));
        } else if (UniProtMediaType.TSV_MEDIA_TYPE.equals(contentType)) {
            addTSVResultMatcher(resultMatchers, expectedEntryCount);
            resultMatchers.add(
                    result ->
                            assertThat(
                                    "Disease TSV header do not match",
                                    result.getResponse()
                                            .getContentAsString()
                                            .startsWith(
                                                    "Name\tDisease ID\tMnemonic\tDescription")));
        } else if (UniProtMediaType.LIST_MEDIA_TYPE.equals(contentType)) {
            addListResultMatcher(resultMatchers, expectedEntryCount);
        } else if (UniProtMediaType.OBO_MEDIA_TYPE.equals(contentType)) {
            addOBOResultMatcher(resultMatchers, expectedEntryCount);
        } else if (UniProtMediaType.XLS_MEDIA_TYPE.equals(contentType)) {
            addXLSResultMatcher(resultMatchers, expectedEntryCount);
        }
        return resultMatchers;
    }

    private Map<String, List<String>> addQueryParam(
            Map<String, List<String>> queryParams, String paramName, List<String> values) {
        Map<String, List<String>> updatedQueryParams = new HashMap<>(queryParams);
        updatedQueryParams.put(paramName, values);
        return updatedQueryParams;
    }

    private void addOBOResultMatcher(
            List<ResultMatcher> resultMatchers, Integer expectedEntryCount) {
        resultMatchers.add(
                result ->
                        assertThat(
                                "The number of obo entries in list does not match",
                                result.getResponse()
                                        .getContentAsString()
                                        .split("\\[Term\\]")
                                        .length,
                                is(expectedEntryCount + 1)));

        resultMatchers.add(
                result ->
                        assertThat(
                                "The obo response doesn't contain namespace",
                                result.getResponse()
                                        .getContentAsString()
                                        .contains("default-namespace: uniprot:diseases")));

        resultMatchers.add(
                result ->
                        assertThat(
                                "The obo term doesn't contain id",
                                Arrays.asList(
                                                result.getResponse()
                                                        .getContentAsString()
                                                        .split("\\[Term\\]"))
                                        .stream()
                                        .allMatch(
                                                s ->
                                                        s.contains("id:")
                                                                || s.startsWith(
                                                                        "format-version"))));

        resultMatchers.add(
                result ->
                        assertThat(
                                "The obo term doesn't contain name",
                                Arrays.asList(
                                                result.getResponse()
                                                        .getContentAsString()
                                                        .split("\\[Term\\]"))
                                        .stream()
                                        .allMatch(
                                                s ->
                                                        s.contains("name:")
                                                                || s.startsWith(
                                                                        "format-version"))));
    }

    private void addListResultMatcher(
            List<ResultMatcher> resultMatchers, Integer expectedEntryCount) {
        resultMatchers.add(
                result ->
                        assertThat(
                                "The number of entries in list does not match",
                                result.getResponse().getContentAsString().split("\n").length,
                                is(expectedEntryCount)));
        resultMatchers.add(
                result ->
                        assertThat(
                                "The list item doesn't start with DI-",
                                Arrays.asList(result.getResponse().getContentAsString().split("\n"))
                                        .stream()
                                        .allMatch(s -> s.startsWith("DI-"))));
    }

    private void addTSVResultMatcher(
            List<ResultMatcher> resultMatchers, Integer expectedEntryCount) {
        resultMatchers.add(
                result ->
                        assertThat(
                                "The number of entries does not match",
                                result.getResponse().getContentAsString().split("\n").length,
                                is(expectedEntryCount + 1)));
    }

    private void addXLSResultMatcher(
            List<ResultMatcher> resultMatchers, Integer expectedEntryCount) {
        resultMatchers.add(
                result ->
                        assertThat(
                                "The excel rows count doesn't match",
                                getExcelRowCountAndVerifyContent(result),
                                is(expectedEntryCount + 1))); // with header
    }
}
