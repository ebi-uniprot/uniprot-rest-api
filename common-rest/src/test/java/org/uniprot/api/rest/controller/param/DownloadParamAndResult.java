package org.uniprot.api.rest.controller.param;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

/**
 * @author sahmad
 */
@Data
@Builder
public class DownloadParamAndResult {

    @Singular private Map<String, List<String>> queryParams;

    private MediaType contentType;

    @Singular private List<ResultMatcher> resultMatchers = new ArrayList<>();

    @Override
    public String toString() {
        return "{" + "queryParams=" + queryParams + ", contentType=" + contentType + '}';
    }
}
