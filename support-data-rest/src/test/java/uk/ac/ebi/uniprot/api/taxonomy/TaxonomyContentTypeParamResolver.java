package uk.ac.ebi.uniprot.api.taxonomy;

import org.springframework.http.MediaType;
import uk.ac.ebi.uniprot.api.rest.controller.AbstractContentTypeParamResolver;
import uk.ac.ebi.uniprot.api.rest.controller.ContentTypeParam;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * @author lgonzales
 */
public class TaxonomyContentTypeParamResolver extends AbstractContentTypeParamResolver {

    @Override
    public List<ContentTypeParam> getSuccessContentTypeRequestParamList() {
        ContentTypeParam json = ContentTypeParam.builder().contentType(MediaType.APPLICATION_JSON)
                .resultMatcher(content().string(containsString("\"taxonId\":9606")))
                .build();
        //TODO: Add other content types (xml, tsv.xls...)
        return Collections.singletonList(json);
    }

    @Override
    public List<ContentTypeParam> getBadRequestContentTypeRequestParamList() {
        ContentTypeParam json = ContentTypeParam.builder().contentType(MediaType.APPLICATION_JSON)
                .resultMatcher(jsonPath("$.url",not(isEmptyOrNullString())))
                .resultMatcher(jsonPath("$.messages.*",contains("The taxonomy id value should be a number")))
                .build();

        ContentTypeParam xml = ContentTypeParam.builder().contentType(MediaType.APPLICATION_XML)
                //.resultMatcher(xpath("$.url",not(isEmptyOrNullString())))
                //.resultMatcher(jsonPath("$.messages.*",contains("The taxonomy id value should be a number")))
                .build();

        //TODO: Add other content types (xml, tsv.xls...)
        return Collections.singletonList(json);
    }

    @Override
    public List<ContentTypeParam> getNotFoundContentTypeRequestParamList() {
        ContentTypeParam json = ContentTypeParam.builder().contentType(MediaType.APPLICATION_JSON)
                .resultMatcher(jsonPath("$.url",not(isEmptyOrNullString())))
                .resultMatcher(jsonPath("$.messages.*",contains("Resource not found")))
                .build();
        //TODO: Add other content types (xml, tsv.xls...)
        return Collections.singletonList(json);
    }




    @Override
    public List<ContentTypeParam> searchSuccessContentTypeRequestParamList() {
        ContentTypeParam json = ContentTypeParam.builder().contentType(MediaType.APPLICATION_JSON)
                .resultMatcher(jsonPath("$.results.size()",is(2)))
                .resultMatcher(jsonPath("$.results.*.taxonId",containsInAnyOrder(12,14)))
                .build();
        //TODO: Add other content types (xml, tsv.xls...)
        return Collections.singletonList(json);
    }

    @Override
    public List<ContentTypeParam> searchBadRequestContentTypeRequestParamList() {
        ContentTypeParam json = ContentTypeParam.builder().contentType(MediaType.APPLICATION_JSON)
                .resultMatcher(jsonPath("$.url",not(isEmptyOrNullString())))
                .resultMatcher(jsonPath("$.messages.*",contains("The taxonomy id filter value should be a number")))
                .build();
        //TODO: Add other content types (xml, tsv.xls...)
        return Collections.singletonList(json);
    }

    @Override
    public List<ContentTypeParam> searchNotFoundContentTypeRequestParamList() {
        ContentTypeParam json = ContentTypeParam.builder().contentType(MediaType.APPLICATION_JSON)
                .resultMatcher(jsonPath("$.results.size()",is(0)))
                .resultMatcher(jsonPath("$.facets.size()",is(0)))
                .build();
        //TODO: Add other content types (xml, tsv.xls...)
        return Collections.singletonList(json);
    }

}
