package org.uniprot.api.uniprotkb.controller.download.resolver;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.test.web.servlet.ResultMatcher;
import org.uniprot.api.uniprotkb.output.converter.UniProtKBXmlMessageConverter;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.uniprot.api.uniprotkb.controller.download.IT.BaseUniprotKBDownloadIT.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class UniProtKBDownloadSortParamAndResultProvider extends UniProtKBDownloadParamAndResultProvider {

    @Override
    protected List<ResultMatcher> getGFFResultMatchers(Integer expectedEntryCount) {
        List<ResultMatcher> resultMatchers = super.getGFFResultMatchers(expectedEntryCount);
        return resultMatchers;
    }

    @Override
    protected List<ResultMatcher> getFastaResultMatchers(Integer expectedEntryCount) {
        List<ResultMatcher> resultMatchers = super.getFastaResultMatchers(expectedEntryCount);

        ResultMatcher sortResultMatcher = result -> {
            String fastaStr = result.getResponse().getContentAsString();
            List<String> lines = Arrays.asList(fastaStr.split("\n"));
            List<String> accessions = lines.stream().filter(l -> l.startsWith(">sp"))
                    .map(l -> l.split("|")[1]).collect(Collectors.toList());
            assertThat(accessions, is(Long.valueOf(expectedEntryCount)));
        };
        resultMatchers.add(sortResultMatcher);

        return resultMatchers;
    }

    @Override
    protected List<ResultMatcher> getXMLResultMatchers(Integer expectedEntryCount) {
        List<ResultMatcher> resultMatchers = super.getXMLResultMatchers(expectedEntryCount);

        ResultMatcher sortResultMatcher =
                result -> {
                    String xmlStr = result.getResponse().getContentAsString();
                    // get string(entry tags) between header and footer
                    String entries =
                            xmlStr.substring(
                                    UniProtKBXmlMessageConverter.HEADER.length(),
                                    xmlStr.indexOf(UniProtKBXmlMessageConverter.FOOTER));
                    List<String> actualAccessions = Arrays.stream(entries.split("\n"))
                            .filter(row -> row.startsWith("  <accession>"))
                            .map(accTag -> accTag.substring("  <accession>".length(), accTag.indexOf("</")))
                            .collect(Collectors.toList());
                    assertThat(actualAccessions, equalTo(SORTED_BY_LENGTH));
                };

        resultMatchers.add(sortResultMatcher);
        return resultMatchers;
    }

    @Override
    protected List<ResultMatcher> getOBOResultMatchers(Integer expectedEntryCount) {
        List<ResultMatcher> resultMatchers = super.getOBOResultMatchers(expectedEntryCount);
        return resultMatchers;
    }

    @Override
    protected List<ResultMatcher> getFFResultMatchers(Integer expectedEntryCount) {
        List<ResultMatcher> resultMatchers = super.getFFResultMatchers(expectedEntryCount);
        ResultMatcher sortResultMatcher =
                result ->{
                    String resp = result.getResponse().getContentAsString();
                    List<String> actualAccessions = Arrays.stream(resp.split("\n"))
                            .filter(row -> row.startsWith("AC   "))
                            .map(row -> row.split("   "))
                            .flatMap(Arrays::stream)
                            .filter(part -> !"AC".equals(part))
                            .map(acc -> acc.split(";"))
                            .flatMap(Arrays::stream)
                            .filter(part -> !";".equals(part))
                            .collect(Collectors.toList());
                    assertThat(actualAccessions, equalTo(SORTED_BY_LENGTH));
                };
        resultMatchers.add(sortResultMatcher);
        return resultMatchers;
    }

    @Override
    protected List<ResultMatcher> getRDFResultMatchers(Integer expectedEntryCount) {
        List<ResultMatcher> resultMatchers = super.getRDFResultMatchers(expectedEntryCount);
        return resultMatchers;
    }

    @Override
    protected List<ResultMatcher> getXLSResultMatchers(Integer expectedEntryCount) {
        List<ResultMatcher> resultMatchers = super.getXLSResultMatchers(expectedEntryCount);

        ResultMatcher sortResultMatcher = result -> assertThat(ACCESSIONS, equalTo(SORTED_BY_LENGTH));
        resultMatchers.add(sortResultMatcher);
        return resultMatchers;
    }

    @Override
    protected List<ResultMatcher> getListResultMatchers(Integer expectedEntryCount) {
        List<ResultMatcher> resultMatchers = super.getListResultMatchers(expectedEntryCount);
        ResultMatcher sortResultMatcher =
                result ->{
                    String resp = result.getResponse().getContentAsString();
                    List<String> actualAccessions = Arrays.stream(resp.split("\n"))
                            .map(row -> row.trim())
                            .collect(Collectors.toList());
                    assertThat(actualAccessions, equalTo(SORTED_BY_LENGTH));
                };
        resultMatchers.add(sortResultMatcher);

        return resultMatchers;
    }

    @Override
    protected List<ResultMatcher> getTSVResultMatchers(Integer expectedEntryCount) {
        List<ResultMatcher> resultMatchers = super.getTSVResultMatchers(expectedEntryCount);

        ResultMatcher sortResultMatcher =
                result ->{
                    String resp = result.getResponse().getContentAsString();
                    List<String> actualAccessions = Arrays.stream(resp.split("\n"))
                            .skip(1)
                            .map(row -> row.split("\t")[0])
                            .collect(Collectors.toList());

                    assertThat(actualAccessions, equalTo(SORTED_BY_LENGTH));
        };
        resultMatchers.add(sortResultMatcher);

        return resultMatchers;
    }

    @Override
    protected List<ResultMatcher> getJsonResultMatchers(Integer expectedEntryCount) {
        List<ResultMatcher> resultMatchers = super.getJsonResultMatchers(expectedEntryCount);
        ResultMatcher sortResultMatcher = jsonPath("$.results[*].primaryAccession", equalTo(SORTED_BY_LENGTH));
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
        ACCESSIONS.remove(0);// remove the header
    }
}
