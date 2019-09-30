package org.uniprot.api.rest.controller.param;

import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import org.springframework.test.web.servlet.ResultMatcher;

/** @author lgonzales */
@Data
@Builder
public class SearchParameter {

    @Singular private Map<String, List<String>> queryParams;

    @Singular private List<ResultMatcher> resultMatchers;
}
