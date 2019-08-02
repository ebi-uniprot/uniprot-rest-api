package uk.ac.ebi.uniprot.api.keyword;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.restdocs.operation.Operation;
import org.springframework.restdocs.operation.OperationRequest;
import org.springframework.restdocs.operation.Parameters;
import org.springframework.restdocs.snippet.Snippet;
import org.springframework.restdocs.snippet.TemplatedSnippet;
import org.springframework.util.StringUtils;
import uk.ac.ebi.uniprot.search.field.KeywordField;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Used in combination with {@code org/springframework/restdocs/templates/asciidoctor/r-request.snippet} to produce
 * concrete {@code r-request.snippet}s, which document an example REST call using the R language for a REST Doc test case.
 * To modify formatting, change {@code r-request.snippet}.
 * <p>
 * Created 18/07/19
 *
 * @author Edd
 */
public class QueryFieldsSnippet extends TemplatedSnippet {
    private KeywordField.Search[] values;

    public QueryFieldsSnippet(KeywordField.Search[] values) {
        super("allowed-fields-in-search-query", null);
        this.values = values;
    }


    public static Snippet info(KeywordField.Search[] values) {
        return new QueryFieldsSnippet(values);
    }


    @Override
    protected Map<String, Object> createModel(Operation operation) {
        Map<String, Object> model = new HashMap<>();
       // model.put("url", this.getUrl(operation));
        model.put("fields", getFields());
        return model;
    }

    private List<KeywordField.Search> getFields() {
        List<KeywordField.Search> f = new ArrayList<>();
        for (KeywordField.Search s : values) {
            f.add(s);
        }
        return f;
    }

    private String getUrl(Operation operation) {
        OperationRequest request = operation.getRequest();
        Parameters uniqueParameters = request.getParameters().getUniqueParameters(operation.getRequest().getUri());
        return !uniqueParameters.isEmpty() && this.includeParametersInUri(request) ? String
                .format("'%s%s%s'", request.getUri(), StringUtils
                        .hasText(request.getUri().getRawQuery()) ? "&" : "?", uniqueParameters.toQueryString()) : String
                .format("'%s'", request.getUri());
    }

    private boolean includeParametersInUri(OperationRequest request) {
        return request.getMethod() == HttpMethod.GET || request
                .getContent().length > 0 && !MediaType.APPLICATION_FORM_URLENCODED
                .isCompatibleWith(request.getHeaders().getContentType());
    }

}
