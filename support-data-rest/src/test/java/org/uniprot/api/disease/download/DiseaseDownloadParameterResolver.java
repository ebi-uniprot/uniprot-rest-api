package org.uniprot.api.disease.download;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.Collections;

import org.uniprot.api.rest.controller.param.SearchParameter;
import org.uniprot.api.rest.controller.param.resolver.AbstractDownloadParameterResolver;
import org.uniprot.api.rest.service.BasicSearchService;

public class DiseaseDownloadParameterResolver extends AbstractDownloadParameterResolver {
    @Override
    protected SearchParameter downloadAllParameter() {
        return SearchParameter.builder()
                .queryParam("query", Collections.singletonList("*"))
                .resultMatcher(jsonPath("$.results.length()", is(500)))
                .build();
    }

    @Override
    protected SearchParameter downloadLessThanDefaultBatchSizeParameter() {
        return SearchParameter.builder()
                .queryParam("query", Collections.singletonList("*"))
                .queryParam(
                        "size",
                        Collections.singletonList(
                                String.valueOf(BasicSearchService.DEFAULT_SOLR_BATCH_SIZE - 40)))
                .resultMatcher(
                        jsonPath(
                                "$.results.length()",
                                is(BasicSearchService.DEFAULT_SOLR_BATCH_SIZE - 40)))
                .build();
    }

    @Override
    protected SearchParameter downloadDefaultBatchSizeParameter() {
        return SearchParameter.builder()
                .queryParam("query", Collections.singletonList("*"))
                .queryParam(
                        "size",
                        Collections.singletonList(
                                String.valueOf(BasicSearchService.DEFAULT_SOLR_BATCH_SIZE)))
                .resultMatcher(
                        jsonPath(
                                "$.results.length()",
                                is(BasicSearchService.DEFAULT_SOLR_BATCH_SIZE)))
                .build();
    }

    @Override
    protected SearchParameter downloadMoreThanBatchSizeParameter() {
        return SearchParameter.builder()
                .queryParam("query", Collections.singletonList("*"))
                .queryParam(
                        "size",
                        Collections.singletonList(
                                String.valueOf(BasicSearchService.DEFAULT_SOLR_BATCH_SIZE * 3)))
                .resultMatcher(
                        jsonPath(
                                "$.results.length()",
                                is(BasicSearchService.DEFAULT_SOLR_BATCH_SIZE * 3)))
                .build();
    }

    @Override
    protected SearchParameter downloadSizeLessThanZeroParameter() {
        return SearchParameter.builder()
                .queryParam("query", Collections.singletonList("*"))
                .queryParam("size", Collections.singletonList(String.valueOf(-1)))
                .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                .resultMatcher(jsonPath("$.messages.*", contains("'size' must be greater than 0")))
                .build();
    }

    @Override
    protected SearchParameter downloadWithoutQueryParameter() {
        return SearchParameter.builder()
                .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                .resultMatcher(
                        jsonPath("$.messages.*", contains("'query' is a required parameter")))
                .build();
    }

    @Override
    protected SearchParameter downloadWithBadQueryParameter() {
        return SearchParameter.builder()
                .queryParam("query", Collections.singletonList("random_field:protein"))
                .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                .resultMatcher(
                        jsonPath(
                                "$.messages.*",
                                contains("'random_field' is not a valid search field")))
                .build();
    }
}
