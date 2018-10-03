package uk.ac.ebi.uniprot.uuw.suggester;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.uniprot.uuw.suggester.model.Suggestion;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created 28/09/18
 *
 * @author Edd
 */
public class GOSuggestions {
    private static final Logger LOGGER = LoggerFactory.getLogger(GOSuggestions.class);
    private static final Pattern TYPE_PREFIX = Pattern.compile("((P:|F:|C:).*)");
    private static final Pattern LINE_FORMAT = Pattern.compile(".*[ \t]*;[ \t]*(GO:[0-9]+)[ \t]*;[ \t]*(.*);.*");

    @Parameter(names = {"--output-file", "-o"}, description = "The destination file")
    private String outputFile = "goSuggestions.txt";

    @Parameter(names = {"--go-file", "-i"}, description = "The source GO file", required = true)
    private String sourceFile;

    public static void main(String[] args) {
        GOSuggestions suggestions = new GOSuggestions();
        JCommander.newBuilder()
                .addObject(suggestions)
                .build()
                .parse(args);
        suggestions.createFile();
    }

    private void createFile() {
        try (FileWriter fw = new FileWriter(outputFile, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw);
             BufferedReader in = new BufferedReader(new FileReader(sourceFile))) {

            String line;
            while ((line = in.readLine()) != null) {
                process(line, out);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to create GO suggestions file, " + sourceFile, e);
        }
    }

    void process(String line, PrintWriter out) {
        Suggestion.SuggestionBuilder lineBuilder = Suggestion.builder();
        Matcher matcher = LINE_FORMAT.matcher(line);
        if (matcher.matches()) {
            lineBuilder.id(matcher.group(1))
                    .name(removeTypePrefixFrom(matcher.group(2)));
            out.println(lineBuilder.build().toSuggestionLine());
        }
    }

    private String removeTypePrefixFrom(String line) {
        if (TYPE_PREFIX.matcher(line).matches()) {
            return line.substring(2);
        } else { 
            return line;
        }
    }
}
