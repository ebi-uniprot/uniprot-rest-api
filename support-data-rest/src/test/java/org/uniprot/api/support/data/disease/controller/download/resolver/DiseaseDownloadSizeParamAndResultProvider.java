package org.uniprot.api.support.data.disease.controller.download.resolver;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.uniprot.api.rest.controller.param.DownloadParamAndResult;

public class DiseaseDownloadSizeParamAndResultProvider
        extends DiseaseDownloadParamAndResultProvider {

    @Override
    public DownloadParamAndResult getDownloadParamAndResult(
            MediaType contentType, Integer entryCount) {
        // add the common param and result matcher
        DownloadParamAndResult paramAndResult =
                super.getDownloadParamAndResult(contentType, entryCount);
        // add param
        Map<String, List<String>> updatedQueryParams =
                addQueryParam(
                        paramAndResult.getQueryParams(),
                        "size",
                        Collections.singletonList(String.valueOf(entryCount)));
        paramAndResult.setQueryParams(updatedQueryParams);
        return paramAndResult;
    }

    public Map<String, List<String>> addQueryParam(
            Map<String, List<String>> queryParams, String paramName, List<String> values) {
        Map<String, List<String>> updatedQueryParams = new HashMap<>(queryParams);
        updatedQueryParams.put(paramName, values);
        return updatedQueryParams;
    }
}
