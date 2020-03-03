package org.uniprot.api.disease.download.resolver;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Sheet;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.MediaType;
import org.uniprot.api.rest.controller.param.DownloadParamAndResult;
import org.uniprot.api.rest.controller.param.resolver.AbstractDownloadSizeParamResolver;
import org.uniprot.api.rest.service.BasicSearchService;

public class DiseaseDownloadSizeParamResolver extends AbstractDownloadSizeParamResolver {
    @RegisterExtension
    static DiseaseDownloadParamAndResultProvider paramAndResultProvider =
            new DiseaseDownloadParamAndResultProvider();

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
            MediaType contentType, Integer downloadSize) {
        // add the common param and result matcher
        DownloadParamAndResult paramAndResult =
                paramAndResultProvider.getDownloadParamAndResult(contentType, downloadSize);
        // add size param
        Map<String, List<String>> updatedQueryParams =
                paramAndResultProvider.addQueryParam(
                        paramAndResult.getQueryParams(),
                        "size",
                        Collections.singletonList(String.valueOf(downloadSize)));
        paramAndResult.setQueryParams(updatedQueryParams);
        return paramAndResult;
    }

    @Override
    protected void verifyExcelData(Sheet sheet) {}
}
