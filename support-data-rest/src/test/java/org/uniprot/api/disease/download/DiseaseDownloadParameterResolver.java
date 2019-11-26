package org.uniprot.api.disease.download;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.Arrays;
import java.util.Collections;

import org.springframework.http.MediaType;
import org.uniprot.api.rest.controller.param.DownloadParamAndResult;
import org.uniprot.api.rest.controller.param.resolver.AbstractDownloadParameterResolver;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.service.BasicSearchService;

public class DiseaseDownloadParameterResolver extends AbstractDownloadParameterResolver {

    @Override
    protected DownloadParamAndResult getDownloadAllParamAndResult(MediaType contentType) {
        DownloadParamAndResult.DownloadParamAndResultBuilder builder =
                DownloadParamAndResult.builder()
                        .queryParam("query", Collections.singletonList("*"))
                        .contentType(contentType);

        if (MediaType.APPLICATION_JSON.equals(contentType)) {
            builder.resultMatcher(jsonPath("$.results.length()", is(500)));
        } else if (UniProtMediaType.TSV_MEDIA_TYPE.equals(contentType)) {
            builder.resultMatcher(
                            content()
                                    .string(
                                            containsString(
                                                    "Name\tDisease ID\tMnemonic\tDescription")))
                    .resultMatcher(
                            result ->
                                    assertThat(
                                            "The number of entries does not match",
                                            result.getResponse()
                                                    .getContentAsString()
                                                    .split("\n")
                                                    .length,
                                            is(501)));
        } else if (UniProtMediaType.LIST_MEDIA_TYPE.equals(contentType)) {

            builder.resultMatcher(
                    result ->
                            assertThat(
                                    "The number of entries in list does not match",
                                    result.getResponse().getContentAsString().split("\n").length,
                                    is(500)));
            builder.resultMatcher(
                    result ->
                            assertThat(
                                    "The list item doesn't start with DI-",
                                    Arrays.asList(
                                                    result.getResponse()
                                                            .getContentAsString()
                                                            .split("\n"))
                                            .stream()
                                            .allMatch(s -> s.startsWith("DI-"))));
        } else if (UniProtMediaType.OBO_MEDIA_TYPE.equals(contentType)) {

            builder.resultMatcher(
                    result ->
                            assertThat(
                                    "The obo response doesn't start with correct format",
                                    result.getResponse()
                                            .getContentAsString()
                                            .startsWith("format-version: 1.2")));
            builder.resultMatcher(
                    result ->
                            assertThat(
                                    "The obo response doesn't contain namespace",
                                    result.getResponse()
                                            .getContentAsString()
                                            .contains("default-namespace: uniprot:diseases")));

            builder.resultMatcher(
                    result ->
                            assertThat(
                                    "The number of obo entries in list does not match",
                                    result.getResponse()
                                            .getContentAsString()
                                            .split("\\[Term\\]")
                                            .length,
                                    is(501)));

            builder.resultMatcher(
                    result ->
                            assertThat(
                                    "The obo term doesn't contain id",
                                    Arrays.asList(
                                                    result.getResponse()
                                                            .getContentAsString()
                                                            .split("\\[Term\\]"))
                                            .stream()
                                            .allMatch(
                                                    s ->
                                                            s.contains("id:")
                                                                    || s.startsWith(
                                                                            "format-version"))));

            builder.resultMatcher(
                    result ->
                            assertThat(
                                    "The obo term doesn't contain name",
                                    Arrays.asList(
                                                    result.getResponse()
                                                            .getContentAsString()
                                                            .split("\\[Term\\]"))
                                            .stream()
                                            .allMatch(
                                                    s ->
                                                            s.contains("name:")
                                                                    || s.startsWith(
                                                                            "format-version"))));
        } else if (UniProtMediaType.XLS_MEDIA_TYPE.equals(contentType)) {
            builder.resultMatcher(
                    result ->
                            assertThat(
                                    "The excel response is empty",
                                    result.getResponse().getContentAsString(),
                                    not(isEmptyOrNullString())));
        }

        return builder.build();
    }

    @Override
    protected DownloadParamAndResult getDownloadLessThanDefaultBatchSizeParamAndResult() {
        return DownloadParamAndResult.builder()
                .queryParam("query", Collections.singletonList("*"))
                .queryParam(
                        "size",
                        Collections.singletonList(
                                String.valueOf(BasicSearchService.DEFAULT_SOLR_BATCH_SIZE - 40)))
                .contentType(MediaType.APPLICATION_JSON)
                .resultMatcher(
                        jsonPath(
                                "$.results.length()",
                                is(BasicSearchService.DEFAULT_SOLR_BATCH_SIZE - 40)))
                .build();
    }

    @Override
    protected DownloadParamAndResult getDownloadDefaultBatchSizeParamAndResult() {
        return DownloadParamAndResult.builder()
                .queryParam("query", Collections.singletonList("*"))
                .queryParam(
                        "size",
                        Collections.singletonList(
                                String.valueOf(BasicSearchService.DEFAULT_SOLR_BATCH_SIZE)))
                .contentType(MediaType.APPLICATION_JSON)
                .resultMatcher(
                        jsonPath(
                                "$.results.length()",
                                is(BasicSearchService.DEFAULT_SOLR_BATCH_SIZE)))
                .build();
    }

    @Override
    protected DownloadParamAndResult getDownloadMoreThanBatchSizeParamAndResult() {
        return DownloadParamAndResult.builder()
                .queryParam("query", Collections.singletonList("*"))
                .queryParam(
                        "size",
                        Collections.singletonList(
                                String.valueOf(BasicSearchService.DEFAULT_SOLR_BATCH_SIZE * 3)))
                .contentType(MediaType.APPLICATION_JSON)
                .resultMatcher(
                        jsonPath(
                                "$.results.length()",
                                is(BasicSearchService.DEFAULT_SOLR_BATCH_SIZE * 3)))
                .build();
    }

    @Override
    protected DownloadParamAndResult getDownloadSizeLessThanZeroParamAndResult() {
        return DownloadParamAndResult.builder()
                .queryParam("query", Collections.singletonList("*"))
                .queryParam("size", Collections.singletonList(String.valueOf(-1)))
                .contentType(MediaType.APPLICATION_JSON)
                .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                .resultMatcher(jsonPath("$.messages.*", contains("'size' must be greater than 0")))
                .build();
    }

    @Override
    protected DownloadParamAndResult getDownloadWithoutQueryParamAndResult() {
        return DownloadParamAndResult.builder()
                .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                .contentType(MediaType.APPLICATION_JSON)
                .resultMatcher(
                        jsonPath("$.messages.*", contains("'query' is a required parameter")))
                .build();
    }

    @Override
    protected DownloadParamAndResult getDownloadWithBadQueryParamAndResult() {
        return DownloadParamAndResult.builder()
                .queryParam("query", Collections.singletonList("random_field:protein"))
                .contentType(MediaType.APPLICATION_JSON)
                .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                .resultMatcher(
                        jsonPath(
                                "$.messages.*",
                                contains("'random_field' is not a valid search field")))
                .build();
    }
}
