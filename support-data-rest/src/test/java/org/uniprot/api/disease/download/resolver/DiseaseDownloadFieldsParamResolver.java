package org.uniprot.api.disease.download.resolver;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.*;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;
import org.uniprot.api.rest.controller.param.DownloadParamAndResult;
import org.uniprot.api.rest.controller.param.resolver.AbstractDownloadFieldsResolver;
import org.uniprot.api.rest.output.UniProtMediaType;

public class DiseaseDownloadFieldsParamResolver extends AbstractDownloadFieldsResolver {

    @Override
    protected DownloadParamAndResult getDownloadInvalidFieldsParamAndResult(
            MediaType contentType, List<String> invalidRequestedFields) {
        DownloadParamAndResult paramAndResult = getDownloadDefaultParamAndResult(contentType, 3);
        // request extra fields than the default ones
        Map<String, List<String>> queryParams =
                addQueryParam(
                        paramAndResult.getQueryParams(),
                        "fields",
                        Collections.singletonList(
                                "embl,ebi,cross_references,reviewed_protein_count"));
        paramAndResult.setQueryParams(queryParams);
        // match response
        ResultMatcher fieldM1 = jsonPath("$.url", notNullValue());
        ResultMatcher fieldM2 = jsonPath("$.messages").exists();
        ResultMatcher fieldM3 =
                jsonPath(
                        "$.messages.*",
                        containsInAnyOrder(
                                "Invalid fields parameter value 'embl'",
                                "Invalid fields parameter value 'ebi'"));
        List<ResultMatcher> fieldMatchers = Arrays.asList(fieldM1, fieldM2, fieldM3);
        List<ResultMatcher> matchers = new ArrayList<>(fieldMatchers);
        paramAndResult.setResultMatchers(matchers);
        return paramAndResult;
    }

    @Override
    protected DownloadParamAndResult getDownloadDefaultFieldsParamAndResult(
            MediaType contentType, List<String> expectedFields) {
        return null;
    }

    @Override
    protected DownloadParamAndResult getDownloadNonDefaultFieldsParamAndResult(
            MediaType contentType, List<String> requestedFields, List<String> expectedFields) {
        DownloadParamAndResult paramAndResult = getDownloadDefaultParamAndResult(contentType, 3);
        // request extra fields than the default ones
        Map<String, List<String>> queryParams =
                addQueryParam(
                        paramAndResult.getQueryParams(),
                        "fields",
                        Collections.singletonList("cross_references,reviewed_protein_count"));
        paramAndResult.setQueryParams(queryParams);
        // match response
        ResultMatcher fieldM1 = jsonPath("$.results[*].crossReferences").exists();
        ResultMatcher fieldM2 = jsonPath("$.results[*].reviewedProteinCount").exists();
        // FIXME accession should be returned and exist.  See
        // https://www.ebi.ac.uk/panda/jira/browse/TRM-23245
        ResultMatcher fieldM3 = jsonPath("$.results[*].accession").doesNotExist();
        List<ResultMatcher> fieldMatchers = Arrays.asList(fieldM1, fieldM2, fieldM3);
        List<ResultMatcher> matchers = new ArrayList<>(paramAndResult.getResultMatchers());
        matchers.addAll(fieldMatchers);
        paramAndResult.setResultMatchers(matchers);
        return paramAndResult;
    }

    static List<String> XLS_ACCESSIONS = new ArrayList<>();

    @Override
    protected void verifyExcelData(Sheet sheet) {
        XLS_ACCESSIONS = new ArrayList<>();
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
                    if (i == headerList.indexOf("DiseaseEntry ID")) { // DiseaseEntry ID field
                        XLS_ACCESSIONS.add(cell.getStringCellValue());
                    }
                    i++;
                }
            }
            headerRead = true;
        }
        assertThat(
                "Header of excel file doesn't match",
                headerList.equals(
                        Arrays.asList("Name", "DiseaseEntry ID", "Mnemonic", "Description")));
    }

    private DownloadParamAndResult getDownloadDefaultParamAndResult(
            MediaType contentType, Integer entryCount) {
        // add the common param and result matcher
        DownloadParamAndResult paramAndResult = getCommonDownloadParamAndResult(contentType);
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
        if (MediaType.APPLICATION_JSON.equals(contentType)) { // TODO add exists
            resultMatchers.add(jsonPath("$.results.length()", is(expectedEntryCount)));
        } else if (UniProtMediaType.TSV_MEDIA_TYPE.equals(contentType)) {
            addTSVResultMatcher(resultMatchers, expectedEntryCount);
            resultMatchers.add(
                    result ->
                            assertThat(
                                    "DiseaseEntry TSV header do not match",
                                    result.getResponse()
                                            .getContentAsString()
                                            .startsWith(
                                                    "Name\tDiseaseEntry ID\tMnemonic\tDescription")));
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
