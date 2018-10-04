package uk.ac.ebi.uniprot.uuw.suggester;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.uniprot.uuw.suggester.model.Suggestion;

import java.io.*;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates file used for GO suggestions. Depends on GO.dat file, located in
 * /ebi/uniprot/production/xrefs/prod/current/databases.
 *
 * Created 28/09/18
 *
 * @author Edd
 */
public class GOSuggestions {
    private static final Logger LOGGER = LoggerFactory.getLogger(GOSuggestions.class);
    private static final Pattern TYPE_PREFIX = Pattern.compile("((P:|F:|C:).*)");
    private static final Pattern LINE_FORMAT = Pattern.compile(".*[ \t]*;[ \t]*(GO:[0-9]+)[ \t]*;[ \t]*(.*);.*");

    @Parameter(names = {"--output-file", "-o"}, description = "The destination file")
    private String outputFile = "go-suggestions.txt";

    @Parameter(names = {"--go-file", "-i"}, description = "The source GO file, e.g., /ebi/uniprot/production/xrefs/prod/current/databases/GO.dat", required = true)
    private String sourceFile;

    @Parameter(names = "--help", help = true)
    private boolean help = false;

    public static void main(String[] args) {
        GOSuggestions suggestions = new GOSuggestions();
        JCommander jCommander = JCommander.newBuilder()
                .addObject(suggestions)
                .build();
        jCommander.parse(args);
        if (suggestions.help) {
            jCommander.usage();
            return;
        }
        suggestions.createFile();
    }

    private void createFile() {
        try (FileWriter fw = new FileWriter(outputFile);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw);
             BufferedReader in = new BufferedReader(new FileReader(sourceFile))) {

            String line;
            Set<String> suggestions = new HashSet<>();
            while ((line = in.readLine()) != null) {
                process(line, suggestions);
            }
            suggestions.forEach(out::println);
        } catch (IOException e) {
            LOGGER.error("Failed to create GO suggestions file, " + sourceFile, e);
        }
    }

    void process(String line, Set<String> suggestions) {
        Suggestion.SuggestionBuilder lineBuilder = Suggestion.builder();
        Matcher matcher = LINE_FORMAT.matcher(line);
        if (matcher.matches()) {
            lineBuilder.id(matcher.group(1))
                    .name(removeTypePrefixFrom(matcher.group(2)));
            suggestions.add(lineBuilder.build().toSuggestionLine());
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
