package org.uniprot.api.uniprotkb.controller.download.resolver;

import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.test.web.servlet.ResultMatcher;
import org.uniprot.api.uniprotkb.output.converter.UniProtKBXmlMessageConverter;

public class UniProtKBDownloadFieldsParamAndResultProvider
        extends UniProtKBDownloadParamAndResultProvider {

    @Override
    protected List<ResultMatcher> getGFFResultMatchers(
            Integer entryCount,
            String sortFieldName,
            String sortOrder,
            List<String> accessionsInOrder,
            List<String> requestedFields,
            List<String> expectedFields) {
        List<ResultMatcher> resultMatchers =
                super.getGFFResultMatchers(
                        entryCount,
                        sortFieldName,
                        sortOrder,
                        accessionsInOrder,
                        requestedFields,
                        expectedFields);
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
                                    .collect(toList());
                    assertThat(accessions, equalTo(accessionsInOrder));
                };

        resultMatchers.add(sortResultMatcher);
        return resultMatchers;
    }

    @Override
    protected List<ResultMatcher> getFastaResultMatchers(
            Integer entryCount,
            String sortFieldName,
            String sortOrder,
            List<String> accessionsInOrder,
            List<String> requestedFields,
            List<String> expectedFields) {
        List<ResultMatcher> resultMatchers =
                super.getFastaResultMatchers(
                        entryCount,
                        sortFieldName,
                        sortOrder,
                        accessionsInOrder,
                        requestedFields,
                        expectedFields);

        // fasta always returns the fixed format whether we pass fields or not
        // example
        // >sp|O12345|MNEMONIC_B Phosphoribosylformylglycinamidine synthase subunit PurL OS=Homo sapiens OX=1 GN=cUrl PE=3 SV=1
        //KDVHMPKHPELADKNVPNLHVMKAMQS
        ResultMatcher fieldsResultMatcher =
                result -> {
                    String fastaStr = result.getResponse().getContentAsString();
                    String id = fastaStr.substring(1, fastaStr.indexOf(' '));
                    assertThat("id is null", id != null);
                    String[] idParts = id.split("\\|");
                    assertThat("id format not valid", idParts.length == 3);
                    String rest = fastaStr.substring(fastaStr.indexOf(' '));
                    String[] headSeq = rest.split("\n");
                    assertThat("header and sequence not present", headSeq.length == 2);
                    String header = headSeq[0].trim();
                    String desc = header.substring(0, header.indexOf(" OS="));
                    String keyValPairs = header.substring(desc.length());
                    assertThat("OS not found", keyValPairs.contains("OS"));
                    assertThat("OX not found", keyValPairs.contains("OX"));
                    assertThat("GN not found", keyValPairs.contains("GN"));
                    assertThat("PE not found", keyValPairs.contains("PE"));
                    assertThat("SV not found", keyValPairs.contains("SV"));
                    String sequence = headSeq[1].trim();
                    assertThat("seq null", sequence != null);
                };
        resultMatchers.add(fieldsResultMatcher);

        return resultMatchers;
    }

    @Override
    protected List<ResultMatcher> getXMLResultMatchers(
            Integer entryCount,
            String sortFieldName,
            String sortOrder,
            List<String> accessionsInOrder,
            List<String> requestedFields,
            List<String> expectedFields) {
        List<ResultMatcher> resultMatchers =
                super.getXMLResultMatchers(
                        entryCount,
                        sortFieldName,
                        sortOrder,
                        accessionsInOrder,
                        requestedFields,
                        expectedFields);

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
                                    .collect(toList());
                    assertThat(actualAccessions, equalTo(accessionsInOrder));
                };

        resultMatchers.add(sortResultMatcher);
        return resultMatchers;
    }

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
        return resultMatchers;
    }

    @Override
    protected List<ResultMatcher> getFFResultMatchers(
            Integer entryCount,
            String sortFieldName,
            String sortOrder,
            List<String> accessionsInOrder,
            List<String> requestedFields,
            List<String> expectedFields) {
        List<ResultMatcher> resultMatchers =
                super.getFFResultMatchers(
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
                                    .filter(row -> row.startsWith("AC   "))
                                    .map(row -> row.split("   "))
                                    .flatMap(Arrays::stream)
                                    .filter(part -> !"AC".equals(part))
                                    .map(acc -> acc.split(";"))
                                    .flatMap(Arrays::stream)
                                    .filter(part -> !";".equals(part))
                                    .collect(toList());
                    assertThat(actualAccessions, equalTo(accessionsInOrder));
                };
        resultMatchers.add(sortResultMatcher);
        return resultMatchers;
    }

    @Override
    protected List<ResultMatcher> getRDFResultMatchers(
            Integer entryCount,
            String sortFieldName,
            String sortOrder,
            List<String> accessionsInOrder,
            List<String> requestedFields,
            List<String> expectedFields) {
        List<ResultMatcher> resultMatchers =
                super.getRDFResultMatchers(
                        entryCount,
                        sortFieldName,
                        sortOrder,
                        accessionsInOrder,
                        requestedFields,
                        expectedFields);
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
                    List<String> headers = stream(sheet.spliterator(), false)
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
        ResultMatcher sortResultMatcher =
                result -> {
                    String resp = result.getResponse().getContentAsString();
                    List<String> actualAccessions =
                            Arrays.stream(resp.split("\n"))
                                    .map(row -> row.trim())
                                    .collect(toList());
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
        ResultMatcher accessionResultMatcher =
                jsonPath("$.results[*].primaryAccession", equalTo(accessionsInOrder));
        resultMatchers.add(accessionResultMatcher);
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
