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
import java.util.Comparator;
import java.util.Objects;

/**
 * Generates file used for EC suggestions. Depends on enzyme.dat file produced by SIB.
 *
 * Created 28/09/18
 *
 * @author Edd
 */
public class ECSuggestions {
    private static final Logger LOGGER = LoggerFactory.getLogger(ECSuggestions.class);

    @Parameter(names = {"--output-file", "-o"}, description = "The destination file")
    private String outputFile = "ecSuggestions.txt";

    @Parameter(
            names = {"--enzyme-file-url", "-i"},
            description = "The URL of the enzyme commission file (default is 'ftp://ftp.expasy.org/databases/enzyme/enzyme.dat')")
    private String sourceFile = "ftp://ftp.expasy.org/databases/enzyme/enzyme.dat";

    public static void main(String[] args) throws IOException {
        ECSuggestions suggestions = new ECSuggestions();
        JCommander.newBuilder()
                .addObject(suggestions)
                .build()
                .parse(args);
        suggestions.createFile();
    }

    private void createFile() throws IOException {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        long time = timestamp.getTime();

        Path directory = Files.createTempDirectory("ec-tmp" + time);
        Path tempECFile = Paths.get(directory.toAbsolutePath().toString(), "ec-file.txt");

        addDeleteDirOnExitHook(directory);

        Files.copy(new URL(sourceFile).openStream(), tempECFile);

        try (FileWriter fw = new FileWriter(outputFile);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw);
             BufferedReader in = Files.newBufferedReader(tempECFile)) {
            String line;
            Suggestion.SuggestionBuilder lineBuilder = Suggestion.builder();

            while ((line = in.readLine()) != null) {
                lineBuilder = process(line, lineBuilder, out);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to create EC suggestions file, " + sourceFile, e);
        }
    }

    Suggestion.SuggestionBuilder process(String line, Suggestion.SuggestionBuilder lineBuilder, PrintWriter out) {
        if (line.startsWith("ID")) {
            lineBuilder.id(removePrefixFrom(line));
        } else if (line.startsWith("DE")) {
            lineBuilder.name(removePrefixFrom(line));
        }
        if (line.startsWith("//")) {
            Suggestion suggestion = lineBuilder.build();
            if (Objects.nonNull(suggestion.getId()) && Objects.nonNull(suggestion.getName())) {
                out.println(suggestion.toSuggestionLine());
                lineBuilder = Suggestion.builder();
            }
        }
        return lineBuilder;
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
