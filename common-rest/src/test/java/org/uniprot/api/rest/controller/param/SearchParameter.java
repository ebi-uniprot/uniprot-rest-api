package org.uniprot.api.rest.controller.param;

import java.util.List;
import java.util.Map;

import org.springframework.test.web.servlet.ResultMatcher;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

/**
 * @author lgonzales
 */
@Data
@Builder
public class SearchParameter {

    @Singular private Map<String, List<String>> queryParams;

    @Singular private List<ResultMatcher> resultMatchers;
}
