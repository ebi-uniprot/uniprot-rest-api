package org.uniprot.api.support.data.disease.controller.download.resolver;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.test.web.servlet.ResultMatcher;

public class DiseaseDownloadSortParamAndResultProvider
        extends DiseaseDownloadParamAndResultProvider {

    @Override
    protected List<ResultMatcher> getOBOResultMatchers(
            Integer entryCount,
            String sortFieldName,
            String sortOrder,
            List<String> accessionsInOrder,
            List<String> requestedFields,
            List<String> expectedFields) {
        List<ResultMatcher> resultMatchers =
                super.getOBOResultMatchers(
                        entryCount,
                        sortFieldName,
                        sortOrder,
                        accessionsInOrder,
                        requestedFields,
                        expectedFields);
        ResultMatcher sortResultMatcher =
                result ->
                        assertThat(
                                "DiseaseEntry OBO accession not in order",
                                getAccessionsFromOBO(result.getResponse().getContentAsString()),
                                equalTo(accessionsInOrder));
        resultMatchers.add(sortResultMatcher);
        return resultMatchers;
    }

    @Override
    protected List<ResultMatcher> getXLSResultMatchers(
            Integer entryCount,
            String sortFieldName,
            String sortOrder,
            List<String> accessionsInOrder,
            List<String> requestedFields,
            List<String> expectedFields) {
        List<ResultMatcher> resultMatchers =
                super.getXLSResultMatchers(
                        entryCount,
                        sortFieldName,
                        sortOrder,
                        accessionsInOrder,
                        requestedFields,
                        expectedFields);

        ResultMatcher sortResultMatcher =
                result -> assertThat(ACCESSIONS, equalTo(accessionsInOrder));
        resultMatchers.add(sortResultMatcher);
        return resultMatchers;
    }

    @Override
    protected List<ResultMatcher> getListResultMatchers(
            Integer entryCount,
            String sortFieldName,
            String sortOrder,
            List<String> accessionsInOrder,
            List<String> requestedFields,
            List<String> expectedFields) {
        List<ResultMatcher> resultMatchers =
                super.getListResultMatchers(
                        entryCount,
                        sortFieldName,
                        sortOrder,
                        accessionsInOrder,
                        requestedFields,
                        expectedFields);
        ResultMatcher sortResultMatcher =
                result -> {
                    String resp = result.getResponse().getContentAsString();
                    List<String> actualAccessions =
                            Arrays.stream(resp.split("\n"))
                                    .map(row -> row.trim())
                                    .collect(Collectors.toList());
                    assertThat(actualAccessions, equalTo(accessionsInOrder));
                };
        resultMatchers.add(sortResultMatcher);

        return resultMatchers;
    }

    @Override
    protected List<ResultMatcher> getTSVResultMatchers(
            Integer entryCount,
            String sortFieldName,
            String sortOrder,
            List<String> accessionsInOrder,
            List<String> requestedFields,
            List<String> expectedFields) {
        List<ResultMatcher> resultMatchers =
                super.getTSVResultMatchers(
                        entryCount,
                        sortFieldName,
                        sortOrder,
                        accessionsInOrder,
                        requestedFields,
                        expectedFields);

        ResultMatcher sortResultMatcher =
                result -> {
                    String resp = result.getResponse().getContentAsString();
                    List<String> actualAccessions =
                            Arrays.stream(resp.split("\n"))
                                    .skip(1)
                                    .map(row -> row.split("\t")[1])
                                    .collect(Collectors.toList());

                    assertThat(actualAccessions, equalTo(accessionsInOrder));
                };
        resultMatchers.add(sortResultMatcher);

        return resultMatchers;
    }

    @Override
    protected List<ResultMatcher> getJsonResultMatchers(
            Integer entryCount,
            String sortFieldName,
            String sortOrder,
            List<String> accessionsInOrder,
            List<String> requestedFields,
            List<String> expectedFields) {
        List<ResultMatcher> resultMatchers =
                super.getJsonResultMatchers(
                        entryCount,
                        sortFieldName,
                        sortOrder,
                        accessionsInOrder,
                        requestedFields,
                        expectedFields);
        ResultMatcher sortResultMatcher = jsonPath("$.results[*].id", equalTo(accessionsInOrder));
        resultMatchers.add(sortResultMatcher);
        return resultMatchers;
    }

    private static List<String> ACCESSIONS;

    @Override
    protected void verifyExcelData(Sheet sheet) {
        ACCESSIONS = new ArrayList<>();
        super.verifyExcelData(sheet);
        for (Row row : sheet) {
            ACCESSIONS.add(row.getCell(1).getStringCellValue());
        }
        ACCESSIONS.remove(0); // remove the header
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
