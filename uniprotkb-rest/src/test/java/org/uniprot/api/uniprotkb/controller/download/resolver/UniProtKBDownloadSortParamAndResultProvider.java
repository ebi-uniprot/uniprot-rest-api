package org.uniprot.api.uniprotkb.controller.download.resolver;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.uniprot.api.uniprotkb.controller.download.IT.BaseUniprotKBDownloadIT.*;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.test.web.servlet.ResultMatcher;
import org.uniprot.api.uniprotkb.output.converter.UniProtKBXmlMessageConverter;

public class UniProtKBDownloadSortParamAndResultProvider
        extends UniProtKBDownloadParamAndResultProvider {

    @Override
    protected List<ResultMatcher> getGFFResultMatchers(Integer entryCount, String sortFieldName, String sortOrder, List<String> accessionsInOrder, List<String> requestedFields, List<String> expectedFields) {
        List<ResultMatcher> resultMatchers = super.getGFFResultMatchers(entryCount, sortFieldName, sortOrder, accessionsInOrder, requestedFields, expectedFields);
        ResultMatcher sortResultMatcher =
                result -> {
                    String gffStr = result.getResponse().getContentAsString();
                    List<String> lines = Arrays.asList(gffStr.split("\n"));
                    Predicate<String> isAccession =
                            l -> l.startsWith("O") || l.startsWith("P") || l.startsWith("Q");
                    List<String> accessions =
                            lines.stream()
                                    .filter(isAccession)
                                    .map(l -> l.substring(0, l.indexOf("\t"))) // Stream<Accession>
                                    .collect(
                                            Collectors.groupingBy(
                                                    Function.identity(),
                                                    LinkedHashMap::new,
                                                    Collectors.counting()))
                                    .keySet()
                                    .stream()
                                    .collect(Collectors.toList());
                    assertThat(accessions, equalTo(accessionsInOrder));
                };

        resultMatchers.add(sortResultMatcher);
        return resultMatchers;
    }

    @Override
    protected List<ResultMatcher> getFastaResultMatchers(Integer entryCount, String sortFieldName, String sortOrder, List<String> accessionsInOrder, List<String> requestedFields, List<String> expectedFields) {
        List<ResultMatcher> resultMatchers = super.getFastaResultMatchers(entryCount, sortFieldName, sortOrder, accessionsInOrder, requestedFields, expectedFields);

        ResultMatcher sortResultMatcher =
                result -> {
                    String fastaStr = result.getResponse().getContentAsString();
                    List<String> lines = Arrays.asList(fastaStr.split("\n"));
                    List<String> accessions =
                            lines.stream()
                                    .filter(l -> l.startsWith(">sp"))
                                    .map(l -> l.split("\\|")[1])
                                    .collect(Collectors.toList());
                    assertThat(accessions, equalTo(accessionsInOrder));
                };
        resultMatchers.add(sortResultMatcher);

        return resultMatchers;
    }

    @Override
    protected List<ResultMatcher> getXMLResultMatchers(Integer entryCount, String sortFieldName, String sortOrder, List<String> accessionsInOrder, List<String> requestedFields, List<String> expectedFields) {
        List<ResultMatcher> resultMatchers = super.getXMLResultMatchers(entryCount, sortFieldName, sortOrder, accessionsInOrder, requestedFields, expectedFields);

        ResultMatcher sortResultMatcher =
                result -> {
                    String xmlStr = result.getResponse().getContentAsString();
                    // get string(entry tags) between header and footer
                    String entries =
                            xmlStr.substring(
                                    UniProtKBXmlMessageConverter.HEADER.length(),
                                    xmlStr.indexOf(UniProtKBXmlMessageConverter.FOOTER));
                    List<String> actualAccessions =
                            Arrays.stream(entries.split("\n"))
                                    .filter(row -> row.startsWith("  <accession>"))
                                    .map(
                                            accTag ->
                                                    accTag.substring(
                                                            "  <accession>".length(),
                                                            accTag.indexOf("</")))
                                    .collect(Collectors.toList());
                    assertThat(actualAccessions, equalTo(accessionsInOrder));
                };

        resultMatchers.add(sortResultMatcher);
        return resultMatchers;
    }

    @Override
    protected List<ResultMatcher> getOBOResultMatchers(Integer entryCount, String sortFieldName, String sortOrder, List<String> accessionsInOrder, List<String> requestedFields, List<String> expectedFields) {
        List<ResultMatcher> resultMatchers = super.getOBOResultMatchers(entryCount, sortFieldName, sortOrder, accessionsInOrder, requestedFields, expectedFields);
        return resultMatchers;
    }

    @Override
    protected List<ResultMatcher> getFFResultMatchers(Integer entryCount, String sortFieldName, String sortOrder, List<String> accessionsInOrder, List<String> requestedFields, List<String> expectedFields) {
        List<ResultMatcher> resultMatchers = super.getFFResultMatchers(entryCount, sortFieldName, sortOrder, accessionsInOrder, requestedFields, expectedFields);
        ResultMatcher sortResultMatcher =
                result -> {
                    String resp = result.getResponse().getContentAsString();
                    List<String> actualAccessions =
                            Arrays.stream(resp.split("\n"))
                                    .filter(row -> row.startsWith("AC   "))
                                    .map(row -> row.split("   "))
                                    .flatMap(Arrays::stream)
                                    .filter(part -> !"AC".equals(part))
                                    .map(acc -> acc.split(";"))
                                    .flatMap(Arrays::stream)
                                    .filter(part -> !";".equals(part))
                                    .collect(Collectors.toList());
                    assertThat(actualAccessions, equalTo(accessionsInOrder));
                };
        resultMatchers.add(sortResultMatcher);
        return resultMatchers;
    }

    @Override
    protected List<ResultMatcher> getRDFResultMatchers(Integer entryCount, String sortFieldName, String sortOrder, List<String> accessionsInOrder, List<String> requestedFields, List<String> expectedFields) {
        List<ResultMatcher> resultMatchers = super.getRDFResultMatchers(entryCount, sortFieldName, sortOrder, accessionsInOrder, requestedFields, expectedFields);
        return resultMatchers;
    }

    @Override
    protected List<ResultMatcher> getXLSResultMatchers(Integer entryCount, String sortFieldName, String sortOrder, List<String> accessionsInOrder, List<String> requestedFields, List<String> expectedFields) {
        List<ResultMatcher> resultMatchers = super.getXLSResultMatchers(entryCount, sortFieldName, sortOrder, accessionsInOrder, requestedFields, expectedFields);

        ResultMatcher sortResultMatcher =
                result -> assertThat(ACCESSIONS, equalTo(accessionsInOrder));
        resultMatchers.add(sortResultMatcher);
        return resultMatchers;
    }

    @Override
    protected List<ResultMatcher> getListResultMatchers(Integer entryCount, String sortFieldName, String sortOrder, List<String> accessionsInOrder, List<String> requestedFields, List<String> expectedFields) {
        List<ResultMatcher> resultMatchers = super.getListResultMatchers(entryCount, sortFieldName, sortOrder, accessionsInOrder, requestedFields, expectedFields);
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
    protected List<ResultMatcher> getTSVResultMatchers(Integer entryCount, String sortFieldName, String sortOrder, List<String> accessionsInOrder, List<String> requestedFields, List<String> expectedFields) {
        List<ResultMatcher> resultMatchers = super.getTSVResultMatchers(entryCount, sortFieldName, sortOrder, accessionsInOrder, requestedFields, expectedFields);

        ResultMatcher sortResultMatcher =
                result -> {
                    String resp = result.getResponse().getContentAsString();
                    List<String> actualAccessions =
                            Arrays.stream(resp.split("\n"))
                                    .skip(1)
                                    .map(row -> row.split("\t")[0])
                                    .collect(Collectors.toList());

                    assertThat(actualAccessions, equalTo(accessionsInOrder));
                };
        resultMatchers.add(sortResultMatcher);

        return resultMatchers;
    }

    @Override
    protected List<ResultMatcher> getJsonResultMatchers(Integer entryCount, String sortFieldName, String sortOrder, List<String> accessionsInOrder, List<String> requestedFields, List<String> expectedFields) {
        List<ResultMatcher> resultMatchers = super.getJsonResultMatchers(entryCount, sortFieldName, sortOrder, accessionsInOrder, requestedFields, expectedFields);
        ResultMatcher sortResultMatcher =
                jsonPath("$.results[*].primaryAccession", equalTo(accessionsInOrder));
        resultMatchers.add(sortResultMatcher);
        return resultMatchers;
    }

    private static List<String> ACCESSIONS;

    @Override
    protected void verifyExcelData(Sheet sheet) {
        ACCESSIONS = new ArrayList<>();
        super.verifyExcelData(sheet);
        for (Row row : sheet) {
            ACCESSIONS.add(row.getCell(0).getStringCellValue());
        }
        ACCESSIONS.remove(0); // remove the header
    }
}
