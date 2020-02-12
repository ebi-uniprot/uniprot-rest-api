package org.uniprot.api.disease.download.resolver;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.uniprot.api.disease.download.IT.BaseDiseaseDownloadIT.*;

import java.util.*;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;
import org.uniprot.api.rest.controller.param.DownloadParamAndResult;
import org.uniprot.api.rest.controller.param.resolver.AbstractDownloadSortParamResolver;
import org.uniprot.api.rest.output.UniProtMediaType;

public class DiseaseDownloadSortParamResolver extends AbstractDownloadSortParamResolver {

    @Override
    protected DownloadParamAndResult getDownloadWithSortParamAndResult(MediaType contentType) {
        DownloadParamAndResult paramAndResult = getDownloadDefaultParamAndResult(contentType, 3);
        // add result matcher to match sorter accession
        List<String> sortedAccessions = Arrays.asList(ACC1, ACC2, ACC3);
        Collections.sort(sortedAccessions, Collections.reverseOrder());

        // add sorting/order related matching
        ResultMatcher sortResultMatcher = null;
        if (MediaType.APPLICATION_JSON.equals(contentType)) {
            sortResultMatcher = jsonPath("$.results[*].accession", equalTo(sortedAccessions));
        } else if (UniProtMediaType.TSV_MEDIA_TYPE.equals(contentType)) {
            sortResultMatcher =
                    result ->
                            assertThat(
                                    "DiseaseEntry TSV accession not in order",
                                    getAccessionsFromTSV(result.getResponse().getContentAsString()),
                                    equalTo(sortedAccessions));
        } else if (UniProtMediaType.LIST_MEDIA_TYPE.equals(contentType)) {
            sortResultMatcher =
                    result ->
                            assertThat(
                                    "DiseaseEntry List accession not in order",
                                    Arrays.asList(
                                            result.getResponse().getContentAsString().split("\n")),
                                    equalTo(sortedAccessions));
        } else if (UniProtMediaType.OBO_MEDIA_TYPE.equals(contentType)) {
            sortResultMatcher =
                    result ->
                            assertThat(
                                    "DiseaseEntry OBO accession not in order",
                                    getAccessionsFromOBO(result.getResponse().getContentAsString()),
                                    equalTo(sortedAccessions));
        } else if (UniProtMediaType.XLS_MEDIA_TYPE.equals(contentType)) {
            sortResultMatcher =
                    result ->
                            assertThat(
                                    "DiseaseEntry Excel accession not in order",
                                    XLS_ACCESSIONS,
                                    equalTo(sortedAccessions));
        }

        if (sortResultMatcher != null) {
            List<ResultMatcher> resultMatchers =
                    new ArrayList<>(paramAndResult.getResultMatchers());
            resultMatchers.add(sortResultMatcher);
            paramAndResult.setResultMatchers(resultMatchers);
        }
        // add param
        Map<String, List<String>> updatedQueryParams =
                addQueryParam(
                        paramAndResult.getQueryParams(),
                        "sort",
                        Collections.singletonList("accession desc"));

        paramAndResult.setQueryParams(updatedQueryParams);
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

    private List<String> getAccessionsFromTSV(String contentAsString) {
        String[] rows = contentAsString.split("\n");
        List<String> accessions = new ArrayList<>();
        // ignore the header and get accession from second tab
        for (String row : Arrays.copyOfRange(rows, 1, rows.length)) {
            String acc = row.split("\t")[1];
            accessions.add(acc);
        }
        return accessions;
    }

    private List<String> getAccessionsFromOBO(String rsp) {
        String[] rows = rsp.split("\\[Term\\]");
        List<String> accessions = new ArrayList<>();
        // ignore the header and get accession from second tab
        for (String row : Arrays.copyOfRange(rows, 1, rows.length)) {
            String acc = row.substring(5, 13);
            accessions.add(acc);
        }
        return accessions;
    }
}
