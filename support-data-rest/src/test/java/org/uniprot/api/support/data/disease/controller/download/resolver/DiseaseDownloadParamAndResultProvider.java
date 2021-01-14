package org.uniprot.api.support.data.disease.controller.download.resolver;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.test.web.servlet.ResultMatcher;
import org.uniprot.api.rest.controller.param.resolver.AbstractDownloadParamAndResultProvider;

public class DiseaseDownloadParamAndResultProvider extends AbstractDownloadParamAndResultProvider {
    @Override
    protected List<ResultMatcher> getOBOResultMatchers(
            Integer expectedEntryCount,
            String sortFieldName,
            String sortOrder,
            List<String> accessionsInOrder,
            List<String> requestedFields,
            List<String> expectedFields) {
        List<ResultMatcher> resultMatchers = new ArrayList<>();
        resultMatchers.add(
                result ->
                        assertThat(
                                "The obo response doesn't start with correct format",
                                result.getResponse()
                                        .getContentAsString()
                                        .startsWith("format-version: 1.2")));
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
        return resultMatchers;
    }

    @Override
    protected List<ResultMatcher> getXLSResultMatchers(
            Integer expectedEntryCount,
            String sortFieldName,
            String sortOrder,
            List<String> accessionsInOrder,
            List<String> requestedFields,
            List<String> expectedFields) {
        List<ResultMatcher> resultMatchers = new ArrayList<>();
        resultMatchers.add(
                result ->
                        assertThat(
                                "The excel response is empty",
                                result.getResponse().getContentAsString(),
                                not(isEmptyOrNullString())));
        resultMatchers.add(
                result ->
                        assertThat(
                                "The excel rows count doesn't match",
                                getExcelRowCountAndVerifyContent(result),
                                is(expectedEntryCount + 1))); // with header

        return resultMatchers;
    }

    @Override
    protected List<ResultMatcher> getListResultMatchers(
            Integer expectedEntryCount,
            String sortFieldName,
            String sortOrder,
            List<String> accessionsInOrder,
            List<String> requestedFields,
            List<String> expectedFields) {
        List<ResultMatcher> resultMatchers = new ArrayList<>();
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

        return resultMatchers;
    }

    @Override
    protected List<ResultMatcher> getTSVResultMatchers(
            Integer expectedEntryCount,
            String sortFieldName,
            String sortOrder,
            List<String> accessionsInOrder,
            List<String> requestedFields,
            List<String> expectedFields) {
        List<ResultMatcher> resultMatchers = new ArrayList<>();
        resultMatchers.add(
                result ->
                        assertThat(
                                "The number of entries does not match",
                                result.getResponse().getContentAsString().split("\n").length,
                                is(expectedEntryCount + 1)));
        return resultMatchers;
    }

    @Override
    protected List<ResultMatcher> getJsonResultMatchers(
            Integer expectedEntryCount,
            String sortFieldName,
            String sortOrder,
            List<String> accessionsInOrder,
            List<String> requestedFields,
            List<String> expectedFields) {
        List<ResultMatcher> resultMatchers = new ArrayList<>();
        resultMatchers.add(jsonPath("$.results.length()", is(expectedEntryCount)));
        return resultMatchers;
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
                headerList.equals(
                        Arrays.asList("Name", "DiseaseEntry ID", "Mnemonic", "Description")));
    }

    // unsupported content type

    @Override
    protected List<ResultMatcher> getGFFResultMatchers(
            Integer expectedEntryCount,
            String sortFieldName,
            String sortOrder,
            List<String> accessionsInOrder,
            List<String> requestedFields,
            List<String> expectedFields) {
        throw new UnsupportedOperationException("content type not supported");
    }

    @Override
    protected List<ResultMatcher> getFastaResultMatchers(
            Integer expectedEntryCount,
            String sortFieldName,
            String sortOrder,
            List<String> accessionsInOrder,
            List<String> requestedFields,
            List<String> expectedFields) {
        throw new UnsupportedOperationException("content type not supported");
    }

    @Override
    protected List<ResultMatcher> getXMLResultMatchers(
            Integer expectedEntryCount,
            String sortFieldName,
            String sortOrder,
            List<String> accessionsInOrder,
            List<String> requestedFields,
            List<String> expectedFields) {
        throw new UnsupportedOperationException("content type not supported");
    }

    @Override
    protected List<ResultMatcher> getFFResultMatchers(
            Integer expectedEntryCount,
            String sortFieldName,
            String sortOrder,
            List<String> accessionsInOrder,
            List<String> requestedFields,
            List<String> expectedFields) {
        throw new UnsupportedOperationException("content type not supported");
    }

    @Override
    protected List<ResultMatcher> getRDFResultMatchers(
            Integer expectedEntryCount,
            String sortFieldName,
            String sortOrder,
            List<String> accessionsInOrder,
            List<String> requestedFields,
            List<String> expectedFields) {
        throw new UnsupportedOperationException("content type not supported");
    }
}
