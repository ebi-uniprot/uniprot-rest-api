package org.uniprot.api.support.data.disease.controller.download.resolver;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;
import org.uniprot.api.rest.controller.param.DownloadParamAndResult;

public class DiseaseDownloadQueryParamAndResultProvider
        extends DiseaseDownloadParamAndResultProvider {

    public DownloadParamAndResult getDownloadParamAndResultForQuery(
            MediaType contentType,
            Integer entryCount,
            String solrQuery,
            List<String> expectedAccession) {
        // add the common param and result matcher
        DownloadParamAndResult.DownloadParamAndResultBuilder builder =
                DownloadParamAndResult.builder().contentType(contentType);
        List<ResultMatcher> resultMatchers =
                getResultMatchers(contentType, entryCount, null, null, null, null, null);
        builder.resultMatchers(resultMatchers);
        DownloadParamAndResult paramAndResult = builder.build();
        if (StringUtils.isNotEmpty(solrQuery)) {
            // add param
            Map<String, List<String>> updatedQueryParams =
                    addQueryParam(
                            paramAndResult.getQueryParams(),
                            "query",
                            Collections.singletonList(solrQuery));
            paramAndResult.setQueryParams(updatedQueryParams);
        }
        return paramAndResult;
    }

    public Map<String, List<String>> addQueryParam(
            Map<String, List<String>> queryParams, String paramName, List<String> values) {
        Map<String, List<String>> updatedQueryParams = new HashMap<>(queryParams);
        updatedQueryParams.put(paramName, values);
        return updatedQueryParams;
    }
}
