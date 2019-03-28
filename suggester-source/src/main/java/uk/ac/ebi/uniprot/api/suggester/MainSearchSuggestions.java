package uk.ac.ebi.uniprot.api.suggester;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import one.util.streamex.StreamEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.uniprot.api.suggester.model.Suggestion;
import uk.ac.ebi.uniprot.cv.xdb.UniProtXDbTypes;
import uk.ac.ebi.uniprot.domain.uniprot.comment.CommentType;
import uk.ac.ebi.uniprot.domain.uniprot.feature.FeatureCategory;
import uk.ac.ebi.uniprot.domain.uniprot.feature.FeatureType;

import static uk.ac.ebi.uniprot.api.suggester.model.Suggestion.computeWeightForName;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Generates file used for the main search box suggestions. Depends on {@link FeatureType},
 * {@link CommentType}, {@link FeatureCategory} and {@link uk.ac.ebi.uniprot.domain.DatabaseType}.
 * <p>
 * Created 28/09/18
 *
 * @author Edd
 */
public class MainSearchSuggestions {
    private static final Logger LOGGER = LoggerFactory.getLogger(MainSearchSuggestions.class);

    @Parameter(names = {"--output-file", "-o"}, description = "The destination file")
    private String outputFile = "main-search-suggestions.txt";

    @Parameter(names = "--help", help = true)
    private boolean help = false;

    public static void main(String[] args) throws IOException {
        MainSearchSuggestions suggestions = new MainSearchSuggestions();
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

    static List<Suggestion> databaseSuggestions() {
        return UniProtXDbTypes.INSTANCE.getAllDBXRefTypes().stream()
                .map(type -> {
                    String name = removeTerminalSemiColon(type.getDisplayName());
                    return Suggestion.builder()
                            .prefix("Database")
                            .name(name)
                            .weight(computeWeightForName(name))
                            .build();
                })
                .collect(Collectors.toList());
    }

    <T extends Enum<T>> List<Suggestion> enumToSuggestions(EnumSuggestionFunction<T> typeToSuggestion) {
        return Stream.of(typeToSuggestion.getEnumType().getEnumConstants())
                .map(typeToSuggestion)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private static String removeTerminalSemiColon(String displayName) {
        int charIndex = displayName.indexOf(';');
        if (charIndex < 0) {
            return displayName;
        } else {
            return displayName.substring(0, charIndex);
        }
    }

    private void createFile() {
        try (FileWriter fw = new FileWriter(outputFile);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            StreamEx.of(enumToSuggestions(new FeatureCategoryToSuggestion()))
                    .append(enumToSuggestions(new FeatureTypeToSuggestion()))
                    .append(enumToSuggestions(new CommentTypeToSuggestion()))
                    .append(databaseSuggestions())
                    .map(Suggestion::toSuggestionLine)
                    .sorted(new Suggestion.Comparator())
                    .forEachOrdered(out::println);
        } catch (IOException e) {
            LOGGER.error("Problem writing main search suggestions file.", e);
        }
    }

    interface EnumSuggestionFunction<T> extends Function<T, Optional<Suggestion>> {
        Class<T> getEnumType();
    }

    static class FeatureCategoryToSuggestion implements EnumSuggestionFunction<FeatureCategory> {
        @Override
        public Optional<Suggestion> apply(FeatureCategory value) {
            String name = value.name();
            return Optional.of(Suggestion.builder()
                                       .prefix("Feature category")
                                       .name(name)
                                       .weight(computeWeightForName(name))
                                       .build());
        }

        @Override
        public Class<FeatureCategory> getEnumType() {
            return FeatureCategory.class;
        }
    }

    static class FeatureTypeToSuggestion implements EnumSuggestionFunction<FeatureType> {
        @Override
        public Optional<Suggestion> apply(FeatureType value) {
            String name = value.getDisplayName();
            return Optional.of(Suggestion.builder()
                                       .prefix("Feature type")
                                       .name(name)
                                       .weight(computeWeightForName(name))
                                       .build());
        }

        @Override
        public Class<FeatureType> getEnumType() {
            return FeatureType.class;
        }
    }

    static class CommentTypeToSuggestion implements EnumSuggestionFunction<CommentType> {
        @Override
        public Optional<Suggestion> apply(CommentType value) {
            String name = value.toXmlDisplayName();
            return value == CommentType.UNKNOWN ?
                    Optional.empty() :
                    Optional.of(Suggestion.builder()
                                        .prefix("Comment type")
                                        .name(name)
                                        .weight(computeWeightForName(name))
                                        .build());
        }

        @Override
        public Class<CommentType> getEnumType() {
            return CommentType.class;
        }
    }
}
