package org.uniprot.api.rest.controller.param;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;

import java.util.List;

@Data
@Builder
public class ContentTypeParam {

    private MediaType contentType;

    @Singular
    private List<ResultMatcher> resultMatchers;
}
