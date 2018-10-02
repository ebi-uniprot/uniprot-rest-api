package uk.ac.ebi.uniprot.uuw.suggester;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.uniprot.uuw.suggester.model.Suggestion;

import java.io.*;

/**
 * Created 28/09/18
 *
 * @author Edd
 */
public class KeywordSuggestions {
    private static final Logger LOGGER = LoggerFactory.getLogger(KeywordSuggestions.class);

    @Parameter(names = {"--output-file", "-o"}, description = "The destination file")
    private String outputFile = "keywordSuggestions.txt";

    @Parameter(names = {"--keyword-list-file", "-i"}, description = "The source keyword list file", required = true)
    private String sourceFile;

    public static void main(String[] args) {
        KeywordSuggestions suggestions = new KeywordSuggestions();
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
            Suggestion.SuggestionBuilder lineBuilder = Suggestion.builder();

            while ((line = in.readLine()) != null) {
                if (line.startsWith("ID")) {
                    lineBuilder.name(removePrefixFrom(line));
                } else if (line.startsWith("AC")) {
                    lineBuilder.id(removePrefixFrom(line));
                }
                if (line.startsWith("//")) {
                    out.println(lineBuilder.build().toSuggestionLine());
                    lineBuilder = Suggestion.builder();
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to create keyword suggestions file, " + sourceFile, e);
        }
    }

    private String removePrefixFrom(String line) {
        return line.substring(5);
    }
}
