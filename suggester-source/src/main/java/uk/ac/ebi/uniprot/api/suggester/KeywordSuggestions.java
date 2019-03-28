package uk.ac.ebi.uniprot.api.suggester;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import uk.ac.ebi.uniprot.api.suggester.model.Suggestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static uk.ac.ebi.uniprot.api.suggester.model.Suggestion.computeWeightForName;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates file used for keyword suggestions. Depends on keywlist.txt file, located in
 * /ebi/ftp/private/uniprot/current_release/knowledgebase/complete/docs.
 * <p>
 * Created 28/09/18
 *
 * @author Edd
 */
public class KeywordSuggestions {
    private static final Logger LOGGER = LoggerFactory.getLogger(KeywordSuggestions.class);
    private static final Pattern KW_FORMAT = Pattern.compile("KW-[0]*([0-9]+)");
    @Parameter(names = {"--output-file", "-o"}, description = "The destination file")
    private String outputFile = "keyword-suggestions.txt";
    @Parameter(names = {"--keyword-list-file",
                        "-i"}, description = "The source keyword list file, e.g., /ebi/ftp/private/uniprot/current_release/knowledgebase/complete/docs/keywlist.txt", required = true)
    private String sourceFile;
    @Parameter(names = "--help", help = true)
    private boolean help = false;

    public static void main(String[] args) {
        KeywordSuggestions suggestions = new KeywordSuggestions();
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

    Suggestion.SuggestionBuilder process(String line, Suggestion.SuggestionBuilder lineBuilder, List<String> suggestions) {
        if (line.startsWith("ID") || line.startsWith("IC")) {
            String name = removePrefixFrom(line);
            lineBuilder.name(name);
            lineBuilder.weight(computeWeightForName(name));
        } else if (line.startsWith("AC")) {
            lineBuilder.id(formatId(removePrefixFrom(line)));
        }
        if (line.startsWith("//")) {
            suggestions.add(lineBuilder.build().toSuggestionLine());
            lineBuilder = Suggestion.builder();
        }
        return lineBuilder;
    }

    private void createFile() {
        try (FileWriter fw = new FileWriter(outputFile);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw);
             BufferedReader in = new BufferedReader(new FileReader(sourceFile))) {
            String line;
            Suggestion.SuggestionBuilder lineBuilder = Suggestion.builder();

            List<String> suggestions = new ArrayList<>();
            while ((line = in.readLine()) != null) {
                lineBuilder = process(line, lineBuilder, suggestions);
            }

            suggestions.sort(new Suggestion.Comparator());

            suggestions.stream().forEachOrdered(out::println);
        } catch (IOException e) {
            LOGGER.error("Failed to create keyword suggestions file, " + sourceFile, e);
        }
    }

    private String formatId(String id) {
        Matcher matcher = KW_FORMAT.matcher(id);
        if (matcher.matches()) {
            return matcher.group(1);
        } else {
            return id;
        }
    }

    private String removePrefixFrom(String line) {
        return line.substring(5);
    }
}
