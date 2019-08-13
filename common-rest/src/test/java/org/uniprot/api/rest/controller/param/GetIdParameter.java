package org.uniprot.api.rest.controller.param;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import org.springframework.test.web.servlet.ResultMatcher;

import java.util.List;
/**
 *
 * @author lgonzales
 */
@Data
@Builder
public class GetIdParameter {

    private String id;

    private String fields;

    @Singular
    private List<ResultMatcher> resultMatchers;

}
