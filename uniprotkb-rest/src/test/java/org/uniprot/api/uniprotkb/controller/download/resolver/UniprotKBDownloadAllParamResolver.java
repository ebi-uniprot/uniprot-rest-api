package org.uniprot.api.uniprotkb.controller.download.resolver;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.uniprot.api.rest.controller.AbstractDownloadControllerIT.ENTRY_COUNT;

import java.util.*;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;
import org.uniprot.api.rest.controller.param.DownloadParamAndResult;
import org.uniprot.api.rest.controller.param.resolver.AbstractDownloadAllParamResolver;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.service.RDFService;

public class UniprotKBDownloadAllParamResolver extends AbstractDownloadAllParamResolver {
    @Override
    protected DownloadParamAndResult getDownloadAllParamAndResult(MediaType contentType) {
        return getDownloadDefaultParamAndResult(contentType, ENTRY_COUNT);
    }

    private DownloadParamAndResult getDownloadDefaultParamAndResult(
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
            resultMatchers.add(
                    result ->
                            assertThat(
                                    "UniProt TSV header does not match",
                                    result.getResponse()
                                            .getContentAsString()
                                            .startsWith(
                                                    "Entry\tEntry Name\tReviewed\tProtein names\tGene Names\tOrganism\tLength")));
        } else if (UniProtMediaType.LIST_MEDIA_TYPE.equals(contentType)) {
            addListResultMatcher(resultMatchers, expectedEntryCount);
        } else if (UniProtMediaType.XLS_MEDIA_TYPE.equals(contentType)) {
            addXLSResultMatcher(resultMatchers, expectedEntryCount);
        } else if (UniProtMediaType.RDF_MEDIA_TYPE.equals(contentType)) {
            addRDFResultMatcher(resultMatchers);
        } else if (UniProtMediaType.FF_MEDIA_TYPE.equals(contentType)) {
            addFFResultMatcher(resultMatchers, expectedEntryCount);
        }
        return resultMatchers;
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
