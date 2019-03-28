package uk.ac.ebi.uniprot.api.suggester;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import uk.ac.ebi.uniprot.api.suggester.model.Suggestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static uk.ac.ebi.uniprot.api.suggester.model.Suggestion.computeWeightForName;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates file used for GO suggestions. Depends on GO.dat file, located in
 * /ebi/uniprot/production/xrefs/prod/current/databases.
 *
 * cat /ebi/uniprot/production/xrefs/prod/2019_04/home_made/GO/GO.dat | awk 'BEGIN{FS=";"}{ if ($2 in ids){} else { ids[$2]=$3}}END{ for (x in ids) {print x, ids[x]}}'
 *
 * -> unique GO ids of the format:
 *
 * format:
 * GO:0005832; C:chaperonin-containing ...
 *
 * Created 28/09/18
 *
 * @author Edd
 */
public class GOSuggestions {
    private static final Logger LOGGER = LoggerFactory.getLogger(GOSuggestions.class);
    private static final Pattern LINE_FORMAT = Pattern.compile("[ \t]*(GO:[0-9]+)[ \t]*(.*)");
    private static final Pattern TYPE_PREFIX = Pattern.compile("((P:|F:|C:).*)");
    private static final Pattern GO_ID_FORMAT = Pattern.compile("GO:[0]*([0-9]+)");
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

    void process(String line, Set<String> suggestions) {
        Suggestion.SuggestionBuilder lineBuilder = Suggestion.builder();
        Matcher matcher = LINE_FORMAT.matcher(line);
        if (matcher.matches()) {
            String name = removeTypePrefixFrom(matcher.group(2));
            lineBuilder.id(formatId(matcher.group(1)))
                    .name(name)
                    .weight(computeWeightForName(name));
            suggestions.add(lineBuilder.build().toSuggestionLine());
        }
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

            List<String> orderedSuggestions = new ArrayList<>(suggestions);
            orderedSuggestions.sort(new Suggestion.Comparator());
            orderedSuggestions.forEach(out::println);
        } catch (IOException e) {
            LOGGER.error("Failed to create GO suggestions file, " + sourceFile, e);
        }
    }

    private String formatId(String id) {
        Matcher matcher = GO_ID_FORMAT.matcher(id);
        if (matcher.matches()) {
            return matcher.group(1);
        } else {
            return id;
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
