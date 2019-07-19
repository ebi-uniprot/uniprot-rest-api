package uk.ac.ebi.uniprot.api.keyword;

import org.springframework.restdocs.cli.CommandFormatter;
import org.springframework.restdocs.operation.Operation;
import org.springframework.restdocs.snippet.TemplatedSnippet;
import org.springframework.util.Assert;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.restdocs.cli.CliDocumentation.multiLineFormat;
import static uk.ac.ebi.uniprot.api.keyword.SnippetHelper.pathMatches;

/**
 * Created 18/07/19
 *
 * @author Edd
 */
public class ResourceSnippet extends TemplatedSnippet {
    private final CommandFormatter commandFormatter;
    private final RequestMappingHandlerMapping requestMappingHandler;

    public static ResourceSnippet resource(RequestMappingHandlerMapping requestMappingHandler) {
        return new ResourceSnippet(multiLineFormat(), requestMappingHandler);
    }

    protected ResourceSnippet(CommandFormatter commandFormatter, RequestMappingHandlerMapping requestMappingHandlerMapping) {
        this((Map) null, commandFormatter, requestMappingHandlerMapping);
    }

    protected ResourceSnippet(Map<String, Object> attributes, CommandFormatter commandFormatter, RequestMappingHandlerMapping requestMappingHandlerMapping) {
        super("resource", attributes);
        Assert.notNull(commandFormatter, "Command formatter must not be null");
        this.commandFormatter = commandFormatter;
        this.requestMappingHandler = requestMappingHandlerMapping;
    }

    @Override
    protected Map<String, Object> createModel(Operation operation) {
        Map<String, Object> model = new HashMap<>();
        model.put("resource", this.supportedContentTypes(operation.getRequest().getUri().getPath()));
        return model;
    }

    private String supportedContentTypes(String requestPath) {
        return requestMappingHandler.getHandlerMethods().keySet().stream()
                .map(requestMappingInfo -> requestMappingInfo.getPatternsCondition().getPatterns().stream())
                .flatMap(l -> l)
                .filter(path -> pathMatches(path, requestPath))
                .findFirst()
                .orElse("");
    }
}