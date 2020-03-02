// package org.uniprot.api.uniprotkb.controller.download.util;
//
// import org.springframework.http.MediaType;
// import org.springframework.test.web.servlet.MvcResult;
// import org.springframework.test.web.servlet.ResultMatcher;
// import org.uniprot.api.rest.output.UniProtMediaType;
//
// import java.util.Arrays;
// import java.util.List;
//
// import static org.hamcrest.MatcherAssert.assertThat;
// import static org.hamcrest.Matchers.is;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//
// public class UniProtKBDownloadResultMatcherProvider {
//
//    public static ResultMatcher getResultMatcher(MediaType contentType, Integer
// expectedEntryCount){
//        ResultMatcher rm ;
//        if (MediaType.APPLICATION_JSON.equals(contentType)) {
//            rm = jsonPath("$.results.length()", is(expectedEntryCount));
//        } else if (UniProtMediaType.TSV_MEDIA_TYPE.equals(contentType)) {
//            addTSVResultMatcher(resultMatchers, expectedEntryCount);
//            resultMatchers.add(
//                    result ->
//                            assertThat(
//                                    "UniProt TSV header does not match",
//                                    result.getResponse()
//                                            .getContentAsString()
//                                            .startsWith(
//                                                    "Entry\tEntry Name\tReviewed\tProtein
// names\tGene Names\tOrganism\tLength")));
//        } else if (UniProtMediaType.LIST_MEDIA_TYPE.equals(contentType)) {
//            addListResultMatcher(resultMatchers, expectedEntryCount);
//        } else if (UniProtMediaType.XLS_MEDIA_TYPE.equals(contentType)) {
//            addXLSResultMatcher(resultMatchers, expectedEntryCount);
//        } else if (UniProtMediaType.RDF_MEDIA_TYPE.equals(contentType)) {
//            addRDFResultMatcher(resultMatchers);
//        } else if(UniProtMediaType.FF_MEDIA_TYPE.equals(contentType)){
//            addFFResultMatcher(resultMatchers, expectedEntryCount);
//        }
//
//
//    }
//    public static ResultMatcher getResultMatchers(MediaType contentType){
//        return getResultMatcher(contentType, null);
//
//    }
//
//    private ResultMatcher getTSVResultMatcher(Integer expectedEntryCount) {
//        ResultMatcher rm = result -> {
//            String response = result.getResponse().getContentAsString();
//            assertThat("The number of entries does not match", response.split("\n").length,
// is(expectedEntryCount + 1));
//            assertThat(
//                    "UniProt TSV header does not match", response.startsWith("Entry\tEntry
// Name\tReviewed\tProtein names\tGene Names\tOrganism\tLength"));
//        };
//        return rm;
//    }
//
//    private ResultMatcher getListResultMatcher(Integer expectedEntryCount) {
//        ResultMatcher rm =
//                result ->
//                {
//                    String response = result.getResponse().getContentAsString();
//                    assertThat("The number of entries in list does not match",
// response.split("\n").length, is(expectedEntryCount));
//                    assertThat(
//                            "The list item doesn't start with [OPQ]",
//                            Arrays.asList(response.split("\n"))
//                                    .stream()
//                                    .allMatch(
//                                            s ->
//                                                    s.startsWith("O")
//                                                            || s.startsWith("P")
//                                                            || s.startsWith("Q")));
//                };
//    }
// }
