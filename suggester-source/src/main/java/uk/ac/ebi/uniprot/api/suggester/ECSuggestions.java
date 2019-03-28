package uk.ac.ebi.uniprot.api.suggester;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.uniprot.api.suggester.model.Suggestion;
import uk.ac.ebi.uniprot.cv.ec.EC;
import uk.ac.ebi.uniprot.cv.ec.ECCache;

import static uk.ac.ebi.uniprot.api.suggester.model.Suggestion.computeWeightForName;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Generates file used for EC suggestions. Depends on enzyme.dat file produced by SIB.
 * <p>
 * Created 28/09/18
 *
 * @author Edd
 */
public class ECSuggestions {
    private static final Logger LOGGER = LoggerFactory.getLogger(ECSuggestions.class);
    private static final String FAILED_TO_CREATE_EC_SUGGESTIONS_FILE = "Failed to create EC suggestions file, ";
    private static final String ENZYME_DAT = "enzyme.dat";
    private static final String ENZCLASS_TXT = "enzclass.txt";
    private static final Pattern ENZYME_CLASS_PATTERN = Pattern
            .compile("^([1-9-]\\.) ?([1-9-]\\.) ?([1-9-]\\.) ?([1-9-]) ? +(.*)\\.$");
    @Parameter(names = {"--output-file", "-o"}, description = "The destination file")
    private String outputFile = "ec-suggestions.txt";
    @Parameter(
            names = {"--enzyme-dir-url", "-i"},
            description = "The URL of the directory containing enzyme commission files (default is 'ftp://ftp.expasy.org/databases/enzyme/enzyme.dat|enzclass.txt')")
    private String sourceDir = "ftp://ftp.expasy.org/databases/enzyme/";
    @Parameter(names = "--help", help = true)
    private boolean help = false;

    public static void main(String[] args) throws IOException {
        ECSuggestions ecSuggestions = new ECSuggestions();
        JCommander jCommander = JCommander.newBuilder()
                .addObject(ecSuggestions)
                .build();
        jCommander.parse(args);
        if (ecSuggestions.help) {
            jCommander.usage();
            return;
        }

        ecSuggestions.writeSuggestions(getECs());
    }

    static List<EC> getECs() {
        List<EC> orderedSuggestions = new ArrayList<>(ECCache.INSTANCE.get());
        orderedSuggestions.sort(new ECComparator());
        return orderedSuggestions;
    }

    private void writeSuggestions(List<EC> suggestions) {
        try (FileWriter fw = new FileWriter(outputFile);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            writeSuggestionsToOutputStream(suggestions, out);
        } catch (IOException e) {
            LOGGER.error(FAILED_TO_CREATE_EC_SUGGESTIONS_FILE + sourceDir, e);
        }
    }

    void writeSuggestionsToOutputStream(List<EC> suggestions, PrintWriter out) {
        suggestions.stream().forEachOrdered(ecSuggestion -> {
            Suggestion suggestion = Suggestion.builder()
                    .name(ecSuggestion.label())
                    .weight(computeWeightForName(ecSuggestion.label()))
                    .id(ecSuggestion.id())
                    .build();
            out.println(suggestion.toSuggestionLine());
        });
    }

    private static class ECComparator implements Comparator<EC> {
        @Override
        public int compare(EC o1, EC o2) {
            return (o1.label() + o1.id()).compareTo(o2.label() + o2.id());
        }
    }
}
