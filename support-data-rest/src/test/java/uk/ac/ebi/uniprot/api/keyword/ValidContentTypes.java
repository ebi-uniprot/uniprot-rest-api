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
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.springframework.restdocs.cli.CliDocumentation.multiLineFormat;

/**
 * Created 18/07/19
 *
 * @author Edd
 */
public class ValidContentTypes extends TemplatedSnippet {
    private final CommandFormatter commandFormatter;
    private final RequestMappingHandlerMapping requestMappingHandler;
    private static final Pattern PATH_VARIABLE_PATTERN = Pattern.compile("\\{.*\\}");

    public static ValidContentTypes validContentTypes(RequestMappingHandlerMapping requestMappingHandlerMapping) {
        return new ValidContentTypes(multiLineFormat(), requestMappingHandlerMapping);
    }

    protected ValidContentTypes(CommandFormatter commandFormatter, RequestMappingHandlerMapping requestMappingHandlerMapping) {
        this((Map) null, commandFormatter, requestMappingHandlerMapping);
    }

    protected ValidContentTypes(Map<String, Object> attributes, CommandFormatter commandFormatter, RequestMappingHandlerMapping requestMappingHandlerMapping) {
        super("valid-content-types", attributes);
        Assert.notNull(commandFormatter, "Command formatter must not be null");
        this.commandFormatter = commandFormatter;
        this.requestMappingHandler = requestMappingHandlerMapping;
    }

    @Override
    protected Map<String, Object> createModel(Operation operation) {
        Map<String, Object> model = new HashMap<>();
//        model.put("url", this.getUrl(operation));
        model.put("validContentTypes", this.validContentTypes(operation.getRequest().getUri().getPath()));
        return model;
    }

//    private String getContentType(Operation operation) {
//        return operation.getRequest().getHeaders().getAccept().stream()
//                .map(MimeType::toString)
//                .collect(Collectors.joining(", "));
//    }

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

    private List<String> validContentTypes(String requestPath) {
        return requestMappingHandler.getHandlerMethods().keySet().stream()
                .filter(requestMappingInfo -> requestMappingInfo.getPatternsCondition().getPatterns()
                        .stream().anyMatch(path -> pathMatches(path, requestPath)))
                .map(r -> r.getProducesCondition().getProducibleMediaTypes())
                .flatMap(Collection::stream)
                .map(MimeType::toString)
                .collect(Collectors.toList());
    }

    private boolean pathMatches(String pathFormat, String path) {
        String[] pathFormatParts = pathFormat.split("/");
        String[] pathParts = path.split("/");
        if (pathFormatParts.length != pathParts.length) {
            return false;
        }
        boolean matches = true;
        for (int i = 0; i < pathFormatParts.length; i++) {
            if (!PATH_VARIABLE_PATTERN.matcher(pathFormatParts[i]).matches() && !pathFormatParts[i].equals(pathParts[i])) {
                matches = false;
                break;
            }
        }
        return matches;
    }
}
