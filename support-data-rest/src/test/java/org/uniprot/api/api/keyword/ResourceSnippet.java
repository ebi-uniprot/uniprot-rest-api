package uk.ac.ebi.uniprot.api.keyword;

import org.springframework.restdocs.cli.CommandFormatter;
import org.springframework.restdocs.operation.Operation;
import org.springframework.restdocs.snippet.TemplatedSnippet;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.restdocs.cli.CliDocumentation.multiLineFormat;

/**
 * Used in combination with {@code org/springframework/restdocs/templates/asciidoctor/resource.snippet} to produce
 * concrete {@code resource.snippet}s, which document the actual resource that was accessed by a REST Doc test. For example,
 * {@code /keyword/{keywordId}}. To modify formatting, change {@code resource.snippet}.
 * <p>
 * Created 18/07/19
 *
 * @author Edd
 */
public class ResourceSnippet extends TemplatedSnippet {
    public static ResourceSnippet resource() {
        return new ResourceSnippet(multiLineFormat());
    }

    protected ResourceSnippet(CommandFormatter commandFormatter) {
        this((Map) null, commandFormatter);
    }

    protected ResourceSnippet(Map<String, Object> attributes, CommandFormatter commandFormatter) {
        super("resource", attributes);
        Assert.notNull(commandFormatter, "Command formatter must not be null");
    }

    @Override
    protected Map<String, Object> createModel(Operation operation) {
        Map<String, Object> model = new HashMap<>();
        model.put("resource", operation.getAttributes().get("org.springframework.restdocs.urlTemplate"));
        return model;
    }
}