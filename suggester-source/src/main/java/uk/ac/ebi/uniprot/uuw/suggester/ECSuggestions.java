package uk.ac.ebi.uniprot.uuw.suggester;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.uniprot.uuw.suggester.model.Suggestion;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static uk.ac.ebi.uniprot.uuw.suggester.model.Suggestion.computeWeightForName;

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

        Set<String> suggestions = ecSuggestions.createSuggestionsForEnzymeClassFile();
        suggestions.addAll(ecSuggestions.createSuggestionsForEnzymeDatFile());
        List<String> orderedSuggestions = new ArrayList<>(suggestions);
        Collections.sort(orderedSuggestions);

        ecSuggestions.writeSuggestions(orderedSuggestions);
    }

    Suggestion.SuggestionBuilder processEnzymeDatLine(String line, Suggestion.SuggestionBuilder lineBuilder, Collection<String> suggestions) {
        if (line.startsWith("ID")) {
            lineBuilder.id(removePrefixFrom(line));
        } else if (line.startsWith("DE")) {
            String name = removePrefixFrom(line);
            lineBuilder.name(name);
            lineBuilder.weight(computeWeightForName(name));
        }
        if (line.startsWith("//")) {
            Suggestion suggestion = lineBuilder.build();
            if (Objects.nonNull(suggestion.getId()) && Objects.nonNull(suggestion.getName())) {
                suggestions.add(suggestion.toSuggestionLine());
                lineBuilder = Suggestion.builder();
            }
        }
        return lineBuilder;
    }

    private void writeSuggestions(List<String> suggestions) {
        try (FileWriter fw = new FileWriter(outputFile);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {

            suggestions.stream().forEachOrdered(out::println);
        } catch (IOException e) {
            LOGGER.error(FAILED_TO_CREATE_EC_SUGGESTIONS_FILE + sourceDir, e);
        }
    }

    private Set<String> createSuggestionsForEnzymeClassFile() throws IOException {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        long time = timestamp.getTime();

        Path directory = Files.createTempDirectory("ec-tmp" + time);
        Path tempECFile = Paths.get(directory.toAbsolutePath().toString(), "ec-file-class.txt");

        addDeleteDirOnExitHook(directory);

        Files.copy(new URL(sourceDir + ENZCLASS_TXT).openStream(), tempECFile);

        Set<String> suggestions = new HashSet<>();
        try (BufferedReader in = Files.newBufferedReader(tempECFile)) {
            String line;

            while ((line = in.readLine()) != null) {
                String enzymeLine = processEnzymeClassLine(line);
                if (enzymeLine != null) {
                    suggestions.add(enzymeLine);
                }
            }
        } catch (IOException e) {
            LOGGER.error(FAILED_TO_CREATE_EC_SUGGESTIONS_FILE + sourceDir, e);
        }
        return suggestions;
    }

    String processEnzymeClassLine(String line) {
        Matcher matcher = ENZYME_CLASS_PATTERN.matcher(line);
        if (matcher.matches() && matcher.groupCount() == 5) {
            return Suggestion.builder()
                    .id(matcher.group(1) + matcher.group(2) + matcher.group(3) + matcher.group(4))
                    .name(matcher.group(5))
                    .weight(computeWeightForName(matcher.group(5)))
                    .build()
                    .toSuggestionLine();
        }
        return null;
    }

    private Set<String> createSuggestionsForEnzymeDatFile() throws IOException {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        long time = timestamp.getTime();

        Path directory = Files.createTempDirectory("ec-tmp" + time);
        Path tempECFile = Paths.get(directory.toAbsolutePath().toString(), "ec-file-dat.txt");

        addDeleteDirOnExitHook(directory);

        Files.copy(new URL(sourceDir + ENZYME_DAT).openStream(), tempECFile);

        Set<String> suggestions = new HashSet<>();
        try (BufferedReader in = Files.newBufferedReader(tempECFile)) {
            String line;
            Suggestion.SuggestionBuilder lineBuilder = Suggestion.builder();

            while ((line = in.readLine()) != null) {
                lineBuilder = processEnzymeDatLine(line, lineBuilder, suggestions);
            }
        } catch (IOException e) {
            LOGGER.error(FAILED_TO_CREATE_EC_SUGGESTIONS_FILE + sourceDir, e);
        }
        return suggestions;
    }

    private void addDeleteDirOnExitHook(Path directory) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Files.walk(directory)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);

                LOGGER.debug("deleted temporary directory {}.", directory);
            } catch (IOException e) {
                LOGGER.error("Problem during temporary directory cleanup", e);
            }
        }));
    }

    private String removePrefixFrom(String line) {
        return line.substring(5);
    }
}
