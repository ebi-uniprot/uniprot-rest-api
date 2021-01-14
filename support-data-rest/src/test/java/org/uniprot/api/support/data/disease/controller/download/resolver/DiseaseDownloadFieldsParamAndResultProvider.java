package org.uniprot.api.support.data.disease.controller.download.resolver;

import static java.util.stream.Collectors.*;
import static java.util.stream.StreamSupport.stream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.test.web.servlet.ResultMatcher;

public class DiseaseDownloadFieldsParamAndResultProvider
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

        ResultMatcher oboValueMatcher =
                result -> {
                    String oboResp = result.getResponse().getContentAsString();
                    String oboTerm = oboResp.substring(oboResp.indexOf("[Term]") + 7);
                    Map<String, Long> fieldCountMap =
                            Stream.of(oboTerm.split("\n"))
                                    .map(line -> line.split(":"))
                                    .map(lp -> ImmutablePair.of(lp[0], lp[1]))
                                    .collect(groupingBy(ImmutablePair::getLeft, counting()));
                    assertTrue(fieldCountMap.size() == 5);
                    assertTrue(fieldCountMap.containsKey("id"));
                    assertTrue(fieldCountMap.containsKey("synonym"));
                    assertTrue(fieldCountMap.containsKey("xref"));
                    assertTrue(fieldCountMap.containsKey("def"));
                    assertTrue(fieldCountMap.containsKey("name"));
                };

        resultMatchers.add(oboValueMatcher);

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
                result -> {
                    Sheet sheet = getExcelSheet(result);
                    List<String> headers =
                            stream(sheet.spliterator(), false)
                                    .limit(1)
                                    .flatMap(row -> stream(row.spliterator(), false))
                                    .map(Cell::getStringCellValue)
                                    .collect(toList());
                    assertThat("headers are not equal", expectedFields.equals(headers));
                };
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
        ResultMatcher resultMatcher =
                result -> {
                    String resp = result.getResponse().getContentAsString();
                    List<String> actualAccessions =
                            Arrays.stream(resp.split("\n"))
                                    .map(row -> row.trim())
                                    .collect(toList());
                    assertThat(actualAccessions, equalTo(accessionsInOrder));
                };
        resultMatchers.add(resultMatcher);

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
        List<ResultMatcher> resultMatchers = new ArrayList<>();
        resultMatchers.add(
                result ->
                        assertThat(
                                "The number of entries does not match",
                                result.getResponse().getContentAsString().split("\n").length,
                                is(entryCount + 1)));

        ResultMatcher fieldsResultMatcher =
                result -> {
                    String resp = result.getResponse().getContentAsString();
                    List<String> fields =
                            Arrays.stream(resp.split("\n"))
                                    .limit(1)
                                    .flatMap(row -> Arrays.stream(row.split("\t")))
                                    .collect(toList());

                    assertThat(fields, equalTo(expectedFields));
                };
        resultMatchers.add(fieldsResultMatcher);
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
        if (requestedFields == null
                || requestedFields.isEmpty()
                || requestedFields.contains("id")) {
            ResultMatcher accessionResultMatcher =
                    jsonPath("$.results[*].id", equalTo(accessionsInOrder));
            resultMatchers.add(accessionResultMatcher);
        }
        ResultMatcher fieldNotNull = result -> assertTrue(expectedFields != null);
        resultMatchers.add(fieldNotNull);

        List<ResultMatcher> fieldsExist =
                expectedFields.stream()
                        .map(field -> jsonPath("$.results[*]." + field).exists())
                        .collect(toList());

        resultMatchers.addAll(fieldsExist);
        return resultMatchers;
    }

    private static List<String> ACCESSIONS;

    @Override
    protected void verifyExcelData(Sheet sheet) {
        ACCESSIONS = new ArrayList<>();
        for (Row row : sheet) {
            ACCESSIONS.add(row.getCell(0).getStringCellValue());
        }
        ACCESSIONS.remove(0); // remove the header
    }
}
