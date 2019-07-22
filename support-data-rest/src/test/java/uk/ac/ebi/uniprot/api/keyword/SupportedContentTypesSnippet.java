package uk.ac.ebi.uniprot.api.keyword;

import org.springframework.restdocs.cli.CommandFormatter;
import org.springframework.restdocs.operation.Operation;
import org.springframework.restdocs.snippet.TemplatedSnippet;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static org.springframework.restdocs.cli.CliDocumentation.multiLineFormat;
import static uk.ac.ebi.uniprot.api.keyword.SnippetHelper.pathMatches;

/**
 * Used in combination with {@code org/springframework/restdocs/templates/asciidoctor/supported-content-types.snippet} to produce
 * concrete {@code supported-content-types.snippet}s, which document the content types supported by a REST end-point.
 * To modify formatting, change {@code supported-content-types.snippet}.
 * <p>
 *
 * Created 18/07/19
 *
 * @author Edd
 */
public class SupportedContentTypesSnippet extends TemplatedSnippet {
    private final CommandFormatter commandFormatter;
    private final RequestMappingHandlerMapping requestMappingHandler;

    public static SupportedContentTypesSnippet supportedContentTypes(RequestMappingHandlerMapping requestMappingHandler) {
        return new SupportedContentTypesSnippet(multiLineFormat(), requestMappingHandler);
    }

    protected SupportedContentTypesSnippet(CommandFormatter commandFormatter, RequestMappingHandlerMapping requestMappingHandlerMapping) {
        this((Map) null, commandFormatter, requestMappingHandlerMapping);
    }

    protected SupportedContentTypesSnippet(Map<String, Object> attributes, CommandFormatter commandFormatter, RequestMappingHandlerMapping requestMappingHandlerMapping) {
        super("supported-content-types", attributes);
        Assert.notNull(commandFormatter, "Command formatter must not be null");
        this.commandFormatter = commandFormatter;
        this.requestMappingHandler = requestMappingHandlerMapping;
    }

    @Override
    protected Map<String, Object> createModel(Operation operation) {
        Map<String, Object> model = new HashMap<>();
        model.put("supportedContentTypes", this.supportedContentTypes(operation.getRequest().getUri().getPath()));
        return model;
    }

    private List<String> supportedContentTypes(String requestPath) {
        return requestMappingHandler.getHandlerMethods().keySet().stream()
                .filter(requestMappingInfo -> requestMappingInfo.getPatternsCondition().getPatterns()
                        .stream().anyMatch(path -> pathMatches(path, requestPath)))
                .findFirst()
                .map(r -> r.getProducesCondition().getProducibleMediaTypes())
                .map(r -> r.stream().map(MimeType::toString).collect(Collectors.toList()))
                .orElse(emptyList());
    }
}