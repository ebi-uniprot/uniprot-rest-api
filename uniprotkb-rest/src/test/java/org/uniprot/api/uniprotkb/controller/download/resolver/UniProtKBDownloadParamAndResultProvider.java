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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;
import org.uniprot.api.rest.controller.param.DownloadParamAndResult;
import org.uniprot.api.rest.controller.param.resolver.AbstractDownloadParamAndResultProvider;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.service.RDFService;
import org.uniprot.api.uniprotkb.output.converter.UniProtKBXmlMessageConverter;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UniProtKBDownloadParamAndResultProvider
        extends AbstractDownloadParamAndResultProvider {

    public DownloadParamAndResult getDownloadParamAndResult(
            MediaType contentType, Integer entryCount) {
        // add the common param and result matcher
        DownloadParamAndResult paramAndResult = getCommonDownloadParamAndResult(contentType);
        // add uniprot specific result matcher
        List<ResultMatcher> resultMatchers =
                getUniProtKBSpecificResultMatcher(
                        contentType, paramAndResult.getResultMatchers(), entryCount);
        paramAndResult.setResultMatchers(resultMatchers);
        return paramAndResult;
    }

    private List<ResultMatcher> getUniProtKBSpecificResultMatcher(
            MediaType contentType,
            List<ResultMatcher> oldResultMatchers,
            Integer expectedEntryCount) {

        List<ResultMatcher> resultMatchers = new ArrayList<>(oldResultMatchers);
        if (MediaType.APPLICATION_JSON.equals(contentType)) {
            resultMatchers.add(jsonPath("$.results.length()", is(expectedEntryCount)));
        } else if (UniProtMediaType.TSV_MEDIA_TYPE.equals(contentType)) {
            addTSVResultMatcher(resultMatchers, expectedEntryCount);
        } else if (UniProtMediaType.LIST_MEDIA_TYPE.equals(contentType)) {
            addListResultMatcher(resultMatchers, expectedEntryCount);
        } else if (UniProtMediaType.XLS_MEDIA_TYPE.equals(contentType)) {
            addXLSResultMatcher(resultMatchers, expectedEntryCount);
        } else if (UniProtMediaType.RDF_MEDIA_TYPE.equals(contentType)) {
            addRDFResultMatcher(resultMatchers);
        } else if (UniProtMediaType.FF_MEDIA_TYPE.equals(contentType)) {
            addFFResultMatcher(resultMatchers, expectedEntryCount);
        } else if (MediaType.APPLICATION_XML.equals(contentType)) {
            addXMLResultMatcher(resultMatchers, expectedEntryCount);
        } else if (UniProtMediaType.FASTA_MEDIA_TYPE.equals(contentType)) {
            addFastaResultMatcher(resultMatchers, expectedEntryCount);
        } else if (UniProtMediaType.GFF_MEDIA_TYPE.equals(contentType)) {
            addGFFResultMatcher(resultMatchers, expectedEntryCount);
        }
        return resultMatchers;
    }

    private void addGFFResultMatcher(
            List<ResultMatcher> resultMatchers, Integer expectedEntryCount) {
        resultMatchers.add(
                result -> {
                    String gffStr = result.getResponse().getContentAsString();
                    assertThat("Response is null", Objects.nonNull(gffStr));
                    List<String> lines = Arrays.asList(gffStr.split("\n"));
                    Predicate<String> isAccession =
                            l -> l.startsWith("O") || l.startsWith("P") || l.startsWith("Q");
                    long entryCount =
                            lines.stream()
                                    .filter(isAccession)
                                    .map(l -> l.substring(0, l.indexOf("\t"))) // Stream<Accession>
                                    .distinct()
                                    .count();
                    assertThat(
                            "Expected entry count doesn't match",
                            entryCount,
                            is(Long.valueOf(expectedEntryCount)));
                });
    }

    private void addFastaResultMatcher(
            List<ResultMatcher> resultMatchers, Integer expectedEntryCount) {
        resultMatchers.add(
                result -> {
                    String fastaStr = result.getResponse().getContentAsString();
                    assertThat("Response is null", Objects.nonNull(fastaStr));
                    List<String> lines = Arrays.asList(fastaStr.split("\n"));
                    long entryCount = lines.stream().filter(l -> l.startsWith(">sp")).count();
                    assertThat(
                            "Expected entry count doesn't match",
                            entryCount,
                            is(Long.valueOf(expectedEntryCount)));
                });
    }

    private void addXMLResultMatcher(
            List<ResultMatcher> resultMatchers, Integer expectedEntryCount) {
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
                            is(expectedEntryCount + 1));
                });
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
                                "The list item doesn't start with [OPQ]",
                                Arrays.asList(result.getResponse().getContentAsString().split("\n"))
                                        .stream()
                                        .allMatch(
                                                s ->
                                                        s.startsWith("O")
                                                                || s.startsWith("P")
                                                                || s.startsWith("Q"))));
    }

    private void addTSVResultMatcher(
            List<ResultMatcher> resultMatchers, Integer expectedEntryCount) {
        resultMatchers.add(
                result ->
                        assertThat(
                                "The number of entries does not match",
                                result.getResponse().getContentAsString().split("\n").length,
                                is(expectedEntryCount + 1)));
        resultMatchers.add(
                result ->
                        assertThat(
                                "UniProt TSV header does not match",
                                result.getResponse()
                                        .getContentAsString()
                                        .startsWith(
                                                "Entry\tEntry Name\tReviewed\tProtein names\tGene Names\tOrganism\tLength")));
    }

    private void addRDFResultMatcher(List<ResultMatcher> resultMatchers) {
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

    private void addFFResultMatcher(
            List<ResultMatcher> resultMatchers, Integer expectedEntryCount) {
        resultMatchers.add(
                result ->
                        assertThat(
                                "The number of entries in flat files does not match",
                                result.getResponse().getContentAsString().split("//\n").length,
                                is(expectedEntryCount)));
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
}
