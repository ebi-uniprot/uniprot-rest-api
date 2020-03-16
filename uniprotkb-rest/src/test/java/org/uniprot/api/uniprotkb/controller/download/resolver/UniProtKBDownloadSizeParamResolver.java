package org.uniprot.api.uniprotkb.controller.download.resolver;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.MediaType;
import org.uniprot.api.rest.controller.param.DownloadParamAndResult;
import org.uniprot.api.rest.controller.param.resolver.AbstractDownloadSizeParamResolver;
import org.uniprot.api.rest.service.BasicSearchService;

public class UniProtKBDownloadSizeParamResolver extends AbstractDownloadSizeParamResolver {
    @RegisterExtension
    static UniProtKBDownloadParamAndResultProvider paramAndResultProvider =
            new UniProtKBDownloadParamAndResultProvider();

    @Override
    public DownloadParamAndResult getDownloadLessThanDefaultBatchSizeParamAndResult(
            MediaType contentType) {
        Integer downloadSize = BasicSearchService.DEFAULT_SOLR_BATCH_SIZE - 40;
        DownloadParamAndResult paramAndResult =
                getDownloadParamAndResult(contentType, downloadSize);
        return paramAndResult;
    }

    @Override
    public DownloadParamAndResult getDownloadDefaultBatchSizeParamAndResult(MediaType contentType) {
        Integer downloadSize = BasicSearchService.DEFAULT_SOLR_BATCH_SIZE;
        DownloadParamAndResult paramAndResult =
                getDownloadParamAndResult(contentType, downloadSize);
        return paramAndResult;
    }

    @Override
    public DownloadParamAndResult getDownloadMoreThanBatchSizeParamAndResult(
            MediaType contentType) {
        Integer downloadSize = BasicSearchService.DEFAULT_SOLR_BATCH_SIZE * 3;
        DownloadParamAndResult paramAndResult =
                getDownloadParamAndResult(contentType, downloadSize);
        return paramAndResult;
    }

    @Override
    public DownloadParamAndResult getDownloadSizeLessThanZeroParamAndResult(MediaType contentType) {
        DownloadParamAndResult.DownloadParamAndResultBuilder builder =
                DownloadParamAndResult.builder()
                        .queryParam("query", Collections.singletonList("*"))
                        .queryParam("size", Collections.singletonList(String.valueOf(-1)))
                        .contentType(contentType);

        if (MediaType.APPLICATION_JSON.equals(contentType)) {
            builder.resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(
                            jsonPath("$.messages.*", contains("'size' must be greater than 0")));
        }
        return builder.build();
    }

    private DownloadParamAndResult getDownloadParamAndResult(
            MediaType contentType, Integer entryCount) {
        // add the common param and result matcher
        DownloadParamAndResult paramAndResult =
                paramAndResultProvider.getDownloadParamAndResult(contentType, entryCount);
        // add param
        Map<String, List<String>> updatedQueryParams =
                addQueryParam(
                        paramAndResult.getQueryParams(),
                        "size",
                        Collections.singletonList(String.valueOf(entryCount)));
        paramAndResult.setQueryParams(updatedQueryParams);
        return paramAndResult;
    }

    private Map<String, List<String>> addQueryParam(
            Map<String, List<String>> queryParams, String paramName, List<String> values) {
        Map<String, List<String>> updatedQueryParams = new HashMap<>(queryParams);
        updatedQueryParams.put(paramName, values);
        return updatedQueryParams;
    }
}
