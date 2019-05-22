package uk.ac.ebi.uniprot.api.rest.controller;

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
public class PathParameter {

    private String pathParam;

    @Singular
    private List<ResultMatcher> resultMatchers;

}
