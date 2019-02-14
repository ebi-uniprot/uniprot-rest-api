package uk.ac.ebi.uniprot.uuw.suggester;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import one.util.streamex.StreamEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.uniprot.domain.uniprot.comment.CommentType;
import uk.ac.ebi.uniprot.domain.uniprot.feature.FeatureCategory;
import uk.ac.ebi.uniprot.domain.uniprot.feature.FeatureType;
import uk.ac.ebi.uniprot.uuw.suggester.model.Suggestion;

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
 * {@link CommentType}, {@link FeatureCategory} and {@link DatabaseType}.
 *
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

    private void createFile() {
        try (FileWriter fw = new FileWriter(outputFile);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
//            StreamEx.of(enumToSuggestions(new DatabaseTypeToSuggestion()))
//                    .append(enumToSuggestions(new FeatureCategoryToSuggestion()))
            StreamEx.of(enumToSuggestions(new FeatureCategoryToSuggestion()))
                    .append(enumToSuggestions(new FeatureTypeToSuggestion()))
                    .append(enumToSuggestions(new CommentTypeToSuggestion()))
                    .map(Suggestion::toSuggestionLine)
                    .forEach(out::println);
        } catch (IOException e) {
            LOGGER.error("Problem writing main search suggestions file.", e);
        }
    }

    <T extends Enum<T>> List<Suggestion> enumToSuggestions(EnumSuggestionFunction<T> typeToSuggestion) {
        return Stream.of(typeToSuggestion.getEnumType().getEnumConstants())
                .map(typeToSuggestion)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    interface EnumSuggestionFunction<T> extends Function<T, Optional<Suggestion>> {
        Class<T> getEnumType();
    }

    //TODO: Need to figure out how to add databases to the main search... because database is no longer an ENUM
/*    static class DatabaseTypeToSuggestion implements EnumSuggestionFunction<DatabaseType> {
        @Override
        public Optional<Suggestion> apply(DatabaseType value) {
            return value == DatabaseType.UNKNOWN ?
                    Optional.empty() :
                    Optional.of(Suggestion.builder()
                                        .prefix("Database")
                                        .name(removeTerminalSemiColon(value.getDisplayName()))
                                        .build());
        }

        @Override
        public Class<DatabaseType> getEnumType() {
            return DatabaseType.class;
        }

        private String removeTerminalSemiColon(String displayName) {
            int charIndex = displayName.indexOf(';');
            if (charIndex < 0) {
                return displayName;
            } else {
                return displayName.substring(0, charIndex);
            }
        }
    }*/

    static class FeatureCategoryToSuggestion implements EnumSuggestionFunction<FeatureCategory> {
        @Override
        public Optional<Suggestion> apply(FeatureCategory value) {
            return Optional.of(Suggestion.builder()
                                       .prefix("Feature category")
                                       .name(value.name())
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
            return Optional.of(Suggestion.builder()
                                       .prefix("Feature type")
                                       .name(value.getDisplayName())
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
            return value == CommentType.UNKNOWN ?
                    Optional.empty() :
                    Optional.of(Suggestion.builder()
                                        .prefix("Comment type")
                                        .name(value.toXmlDisplayName())
                                        .build());
        }

        @Override
        public Class<CommentType> getEnumType() {
            return CommentType.class;
        }
    }
}
