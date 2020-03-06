package org.uniprot.api.uniprotkb.controller.download.resolver;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.junit.jupiter.api.TestInstance;
import org.springframework.test.web.servlet.ResultMatcher;
import org.uniprot.api.rest.controller.param.resolver.AbstractDownloadParamAndResultProvider;
import org.uniprot.api.rest.service.RDFService;
import org.uniprot.api.uniprotkb.output.converter.UniProtKBXmlMessageConverter;

public class UniProtKBDownloadParamAndResultProvider
        extends AbstractDownloadParamAndResultProvider {

    @Override
    protected List<ResultMatcher> getGFFResultMatchers(Integer entryCount, String sortFieldName, String sortOrder, List<String> accessionsInOrder, List<String> requestedFields, List<String> expectedFields) {
        List<ResultMatcher> resultMatchers = new ArrayList<>();
        resultMatchers.add(
                result -> {
                    String gffStr = result.getResponse().getContentAsString();
                    assertThat("Response is null", Objects.nonNull(gffStr));
                    List<String> lines = Arrays.asList(gffStr.split("\n"));
                    Predicate<String> isAccession =
                            l -> l.startsWith("O") || l.startsWith("P") || l.startsWith("Q");
                    long expectedEntryCount =
                            lines.stream()
                                    .filter(isAccession)
                                    .map(l -> l.substring(0, l.indexOf("\t"))) // Stream<Accession>
                                    .distinct()
                                    .count();
                    assertThat(
                            "Expected entry count doesn't match",
                            expectedEntryCount,
                            is(Long.valueOf(entryCount)));
                });
        return resultMatchers;
    }

    @Override
    protected List<ResultMatcher> getFastaResultMatchers(Integer entryCount, String sortFieldName, String sortOrder, List<String> accessionsInOrder, List<String> requestedFields, List<String> expectedFields) {
        List<ResultMatcher> resultMatchers = new ArrayList<>();
        resultMatchers.add(
                result -> {
                    String fastaStr = result.getResponse().getContentAsString();
                    assertThat("Response is null", Objects.nonNull(fastaStr));
                    List<String> lines = Arrays.asList(fastaStr.split("\n"));
                    long expectedEntryCount = lines.stream().filter(l -> l.startsWith(">sp")).count();
                    assertThat(
                            "Expected entry count doesn't match",
                            expectedEntryCount,
                            is(Long.valueOf(entryCount)));
                });

        return resultMatchers;
    }

    @Override
    protected List<ResultMatcher> getXMLResultMatchers(Integer entryCount, String sortFieldName, String sortOrder, List<String> accessionsInOrder, List<String> requestedFields, List<String> expectedFields) {
        List<ResultMatcher> resultMatchers = new ArrayList<>();

        resultMatchers.add(
                result -> {
                    String xmlStr = result.getResponse().getContentAsString();
                    assertThat(
                            "Response start tag in incorrect",
                            xmlStr.startsWith(UniProtKBXmlMessageConverter.HEADER));
                    assertThat(
                            "Response end tag in incorrect",
                            xmlStr.endsWith(UniProtKBXmlMessageConverter.FOOTER));
                    // get string(entry tags) between header and footer
                    String entries =
                            xmlStr.substring(
                                    UniProtKBXmlMessageConverter.HEADER.length(),
                                    xmlStr.indexOf(UniProtKBXmlMessageConverter.FOOTER));
                    String[] entryTags = entries.split("<entry dataset=\"Swiss-Prot\"");
                    assertThat(
                            "Expected entry count doesn't match",
                            entryTags.length,
                            is(entryCount + 1));
                });

        return resultMatchers;
    }

    @Override
    protected List<ResultMatcher> getFFResultMatchers(Integer entryCount, String sortFieldName, String sortOrder, List<String> accessionsInOrder, List<String> requestedFields, List<String> expectedFields) {
        List<ResultMatcher> resultMatchers = new ArrayList<>();
        resultMatchers.add(
                result ->
                        assertThat(
                                "The number of entries in flat files does not match",
                                result.getResponse().getContentAsString().split("//\n").length,
                                is(entryCount)));

        return resultMatchers;
    }

    @Override
    protected List<ResultMatcher> getRDFResultMatchers(Integer entryCount, String sortFieldName, String sortOrder, List<String> accessionsInOrder, List<String> requestedFields, List<String> expectedFields) {
        List<ResultMatcher> resultMatchers = new ArrayList<>();
        resultMatchers.add(
                result ->
                        assertThat(
                                "The rdf response does't starts with expected tag.",
                                result.getResponse()
                                        .getContentAsString()
                                        .startsWith(RDFService.RDF_PROLOG)));

        resultMatchers.add(
                result ->
                        assertThat(
                                "The rdf response does't end with expected tag.",
                                result.getResponse()
                                        .getContentAsString()
                                        .trim()
                                        .endsWith(RDFService.RDF_CLOSE_TAG)));
        return resultMatchers;
    }

    @Override
    protected List<ResultMatcher> getXLSResultMatchers(Integer entryCount, String sortFieldName, String sortOrder, List<String> accessionsInOrder, List<String> requestedFields, List<String> expectedFields) {
        List<ResultMatcher> resultMatchers = new ArrayList<>();
        resultMatchers.add(
                result ->
                        assertThat(
                                "The excel rows count doesn't match",
                                getExcelRowCountAndVerifyContent(result),
                                is(entryCount + 1))); // with header
        return resultMatchers;
    }

    @Override
    protected List<ResultMatcher> getListResultMatchers(Integer entryCount, String sortFieldName, String sortOrder, List<String> accessionsInOrder, List<String> requestedFields, List<String> expectedFields) {
        List<ResultMatcher> resultMatchers = new ArrayList<>();

        resultMatchers.add(
                result ->
                        assertThat(
                                "The number of entries in list does not match",
                                result.getResponse().getContentAsString().split("\n").length,
                                is(entryCount)));
        resultMatchers.add(
                result ->
                        assertThat(
                                "The list item doesn't start with [OPQ]",
                                Arrays.asList(result.getResponse().getContentAsString().split("\n"))
                                        .stream()
                                        .allMatch(
                                                s ->
                                                        s.startsWith("O")
                                                                || s.startsWith("P")
                                                                || s.startsWith("Q"))));
        return resultMatchers;
    }

    @Override
    protected List<ResultMatcher> getTSVResultMatchers(Integer entryCount, String sortFieldName, String sortOrder, List<String> accessionsInOrder, List<String> requestedFields, List<String> expectedFields) {
        List<ResultMatcher> resultMatchers = new ArrayList<>();
        resultMatchers.add(
                result ->
                        assertThat(
                                "The number of entries does not match",
                                result.getResponse().getContentAsString().split("\n").length,
                                is(entryCount + 1)));
        resultMatchers.add(
                result ->
                        assertThat(
                                "UniProt TSV header does not match",
                                result.getResponse()
                                        .getContentAsString()
                                        .startsWith(
                                                "Entry\tEntry Name\tReviewed\tProtein names\tGene Names\tOrganism\tLength")));
        return resultMatchers;
    }

    @Override
    protected List<ResultMatcher> getJsonResultMatchers(Integer entryCount, String sortFieldName, String sortOrder, List<String> accessionsInOrder, List<String> requestedFields, List<String> expectedFields) {
        List<ResultMatcher> resultMatchers = new ArrayList<>();
        resultMatchers.add(jsonPath("$.results.length()", is(entryCount)));
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
                        Arrays.asList(
                                "Entry",
                                "Entry Name",
                                "Reviewed",
                                "Protein names",
                                "Gene Names",
                                "Organism",
                                "Length")));
    }

    @Override
    protected List<ResultMatcher> getOBOResultMatchers(Integer entryCount, String sortFieldName, String sortOrder, List<String> accessionsInOrder, List<String> requestedFields, List<String> expectedFields) {
        throw new UnsupportedOperationException("content type not supported");
    }
}
