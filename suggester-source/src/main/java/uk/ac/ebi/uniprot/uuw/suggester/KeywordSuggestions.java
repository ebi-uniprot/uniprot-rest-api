package uk.ac.ebi.uniprot.uuw.suggester;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.uniprot.uuw.suggester.model.Suggestion;

import java.io.*;

/**
 * Generates file used for keyword suggestions. Depends on keywlist.txt file, located in
 * /ebi/ftp/private/uniprot/current_release/knowledgebase/complete/docs.
 *
 * Created 28/09/18
 *
 * @author Edd
 */
public class KeywordSuggestions {
    private static final Logger LOGGER = LoggerFactory.getLogger(KeywordSuggestions.class);

    @Parameter(names = {"--output-file", "-o"}, description = "The destination file")
    private String outputFile = "keywordSuggestions.txt";

    @Parameter(names = {"--keyword-list-file", "-i"}, description = "The source keyword list file, e.g., /ebi/ftp/private/uniprot/current_release/knowledgebase/complete/docs/keywlist.txt", required = true)
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

    private void createFile() {
        try (FileWriter fw = new FileWriter(outputFile);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw);
             BufferedReader in = new BufferedReader(new FileReader(sourceFile))) {
            String line;
            Suggestion.SuggestionBuilder lineBuilder = Suggestion.builder();

            while ((line = in.readLine()) != null) {
                lineBuilder = process(line, lineBuilder, out);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to create keyword suggestions file, " + sourceFile, e);
        }
    }

    Suggestion.SuggestionBuilder process(String line, Suggestion.SuggestionBuilder lineBuilder, PrintWriter out) {
        if (line.startsWith("ID") || line.startsWith("IC")) {
            lineBuilder.name(removePrefixFrom(line));
        } else if (line.startsWith("AC")) {
            lineBuilder.id(removePrefixFrom(line));
        }
        if (line.startsWith("//")) {
            out.println(lineBuilder.build().toSuggestionLine());
            lineBuilder = Suggestion.builder();
        }
        return lineBuilder;
    }

    private String removePrefixFrom(String line) {
        return line.substring(5);
    }
}
