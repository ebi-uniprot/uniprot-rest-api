package org.uniprot.api.uniprotkb.controller.download.resolver;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.*;
import java.util.function.Predicate;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;
import org.uniprot.api.rest.controller.param.DownloadParamAndResult;
import org.uniprot.api.rest.controller.param.resolver.AbstractDownloadSizeParamResolver;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.service.BasicSearchService;

public class UniProtKBDownloadSizeParamResolver extends AbstractDownloadSizeParamResolver {
    public static final String HEADER =
            "<uniprot xmlns=\"https://uniprot.org/uniprot\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"https://uniprot.org/uniprot https://www.uniprot.org/docs/uniprot.xsd\">\n";
    public static final String FOOTER =
            "<copyright>\n"
                    + "Copyrighted by the UniProt Consortium, see https://www.uniprot.org/terms Distributed under the Creative Commons Attribution (CC BY 4.0) License\n"
                    + "</copyright>\n"
                    + "</uniprot>";

    @Override
    protected DownloadParamAndResult getDownloadLessThanDefaultBatchSizeParamAndResult(
            MediaType contentType) {
        Integer downloadSize = BasicSearchService.DEFAULT_SOLR_BATCH_SIZE - 40;
        DownloadParamAndResult paramAndResult =
                getDownloadParamAndResult(contentType, downloadSize);
        return paramAndResult;
    }

    @Override
    protected DownloadParamAndResult getDownloadDefaultBatchSizeParamAndResult(
            MediaType contentType) {
        Integer downloadSize = BasicSearchService.DEFAULT_SOLR_BATCH_SIZE;
        DownloadParamAndResult paramAndResult =
                getDownloadParamAndResult(contentType, downloadSize);
        return paramAndResult;
    }

    @Override
    protected DownloadParamAndResult getDownloadMoreThanBatchSizeParamAndResult(
            MediaType contentType) {
        Integer downloadSize = BasicSearchService.DEFAULT_SOLR_BATCH_SIZE * 3;
        DownloadParamAndResult paramAndResult =
                getDownloadParamAndResult(contentType, downloadSize);
        return paramAndResult;
    }

    @Override
    protected DownloadParamAndResult getDownloadSizeLessThanZeroParamAndResult(
            MediaType contentType) {
        return DownloadParamAndResult.builder()
                .queryParam("query", Collections.singletonList("*"))
                .queryParam("size", Collections.singletonList(String.valueOf(-1)))
                .contentType(contentType)
                .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                .resultMatcher(jsonPath("$.messages.*", contains("'size' must be greater than 0")))
                .build();
    }

    private DownloadParamAndResult getDownloadParamAndResult(
            MediaType contentType, Integer entryCount) {
        // QUERY
        // add the common param and result matcher
        DownloadParamAndResult paramAndResult = getCommonDownloadParamAndResult(contentType);
        // add param
        Map<String, List<String>> updatedQueryParams =
                addQueryParam(
                        paramAndResult.getQueryParams(),
                        "size",
                        Collections.singletonList(String.valueOf(entryCount)));
        paramAndResult.setQueryParams(updatedQueryParams);
        // RESULT MATCHER
        List<ResultMatcher> resultMatchers =
                getSpecificResultMatcher(
                        contentType, paramAndResult.getResultMatchers(), entryCount);
        paramAndResult.setResultMatchers(resultMatchers);
        return paramAndResult;
    }

    private List<ResultMatcher> getSpecificResultMatcher(
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
                                    "UniProt KB Entry TSV header do not match",
                                    result.getResponse()
                                            .getContentAsString()
                                            .startsWith(
                                                    "Entry\tEntry Name\tReviewed\tProtein names\tGene Names\tOrganism\tLength")));
        } else if (UniProtMediaType.LIST_MEDIA_TYPE.equals(contentType)) {
            addListResultMatcher(resultMatchers, expectedEntryCount);
        } else if (UniProtMediaType.XLS_MEDIA_TYPE.equals(contentType)) {
            addXLSResultMatcher(resultMatchers, expectedEntryCount);
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

    private Map<String, List<String>> addQueryParam(
            Map<String, List<String>> queryParams, String paramName, List<String> values) {
        Map<String, List<String>> updatedQueryParams = new HashMap<>(queryParams);
        updatedQueryParams.put(paramName, values);
        return updatedQueryParams;
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

    private void addXMLResultMatcher(
            List<ResultMatcher> resultMatchers, Integer expectedEntryCount) {
        resultMatchers.add(
                result -> {
                    String xmlStr = result.getResponse().getContentAsString();
                    assertThat("Response start tag in incorrect", xmlStr.startsWith(HEADER));
                    assertThat("Response end tag in incorrect", xmlStr.endsWith(FOOTER));
                    // get string(entry tags) between header and footer
                    String entries = xmlStr.substring(HEADER.length(), xmlStr.indexOf(FOOTER));
                    String[] entryTags = entries.split("<entry dataset=\"Swiss-Prot\"");
                    assertThat(
                            "Expected entry count doesn't match",
                            entryTags.length,
                            is(expectedEntryCount + 1));
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

    private static List<String> XLS_ACCESSIONS = new ArrayList<>();

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
                    if (i == headerList.indexOf("Disease ID")) { // Disease ID field
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
