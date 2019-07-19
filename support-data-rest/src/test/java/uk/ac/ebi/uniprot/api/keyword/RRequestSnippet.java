package uk.ac.ebi.uniprot.api.keyword;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.restdocs.cli.CommandFormatter;
import org.springframework.restdocs.operation.Operation;
import org.springframework.restdocs.operation.OperationRequest;
import org.springframework.restdocs.operation.Parameters;
import org.springframework.restdocs.snippet.TemplatedSnippet;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.restdocs.cli.CliDocumentation.multiLineFormat;

/**
 * Created 18/07/19
 *
 * @author Edd
 */
public class RRequestSnippet extends TemplatedSnippet {
    private final CommandFormatter commandFormatter;

    public static RRequestSnippet rRequest() {
        return new RRequestSnippet(multiLineFormat());
    }

    protected RRequestSnippet(CommandFormatter commandFormatter) {
        this((Map) null, commandFormatter);
    }

    protected RRequestSnippet(Map<String, Object> attributes, CommandFormatter commandFormatter) {
        super("r-request", attributes);
        Assert.notNull(commandFormatter, "Command formatter must not be null");
        this.commandFormatter = commandFormatter;
    }

    @Override
    protected Map<String, Object> createModel(Operation operation) {
        Map<String, Object> model = new HashMap<>();
        model.put("url", this.getUrl(operation));
        model.put("contentType", this.getContentType(operation));
        return model;
    }

    private String getContentType(Operation operation) {
        return operation.getRequest().getHeaders().getAccept().stream()
                .map(MimeType::toString)
                .collect(Collectors.joining(", "));
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
