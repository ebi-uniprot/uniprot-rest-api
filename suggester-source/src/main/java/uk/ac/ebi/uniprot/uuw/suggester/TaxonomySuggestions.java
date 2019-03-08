package uk.ac.ebi.uniprot.uuw.suggester;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import lombok.Builder;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.kraken.database.util.DbConnInfos;
import uk.ac.ebi.kraken.database.util.DbConnectionInfo;
import uk.ac.ebi.uniprot.uuw.suggester.model.Suggestion;

import java.io.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptySet;
import static uk.ac.ebi.uniprot.uuw.suggester.model.Suggestion.computeWeightForName;

/**
 * Generates file used for taxonomy suggestions. Depends on a UniProt DB, e.g., SWPREAD, using data from
 * TAXONOMY.V_PUBLIC_NODE.
 * <p>
 * Created 28/09/18
 *
 * @author Edd
 */
public class TaxonomySuggestions {
    static final String NAME_DELIMITER = " ``` ";
    private static final Logger LOGGER = LoggerFactory.getLogger(TaxonomySuggestions.class);
    private static final int STATS_REPORT_CHUNK_SIZE = 10000;
    private static final String ALL_TAX_QUERY = "select t.tax_id, t.ncbi_scientific, t.ncbi_common," +
            "t.sptr_scientific,t.sptr_common,t.sptr_synonym" +
            " from taxonomy.v_public_node t";
    private static final String DEFAULT_TAXON_SYNONYMS_FILE = "default-taxon-synonyms.txt";
    private static final String COMMENT_LINE_PREFIX = "#";
    @Parameter(names = {"--output-file", "-o"}, description = "The destination file")
    private String outputFile = "taxon-suggestions.txt";

    @Parameter(names = {"--tax-connection",
                        "-c"}, description = "The connection details to the taxonomy DB", required = true)
    private String taxonomyConnectionStr;

    @Parameter(names = "--help", help = true)
    private boolean help = false;

    public static void main(String[] args) throws SQLException {
        TaxonomySuggestions suggestions = new TaxonomySuggestions();
        JCommander jCommander = JCommander.newBuilder()
                .addObject(suggestions)
                .build();
        jCommander.parse(args);
        if (suggestions.help) {
            jCommander.usage();
            return;
        }
        Map<String, Suggestion> suggestionMap = new HashMap<>();
        insertSuggestions(suggestionMap, suggestions.loadDefaultSynonyms());
        insertSuggestions(suggestionMap, suggestions.loadFromDB());
        List<String> suggestionLines = suggestionMap.values().stream()
                .map(Suggestion::toSuggestionLine)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        suggestions.print(suggestionLines);
    }

    Set<Suggestion> createTaxSuggestions(TaxEntity taxEntity) {
        // scientific name
        String scientific = taxEntity.getSptrScientific();
        if (Objects.isNull(scientific)) {
            scientific = taxEntity.getNcbiScientific();
        }

        // common name
        String common = taxEntity.getSptrCommon();
        if (Objects.isNull(common)) {
            common = taxEntity.getNcbiCommon();
        }

        // synonym
        String synonym = taxEntity.getSptrSynonym();

        return createSuggestions(taxEntity.getTaxId(), scientific, common, synonym);
    }

    private static void insertSuggestions(Map<String, Suggestion> suggestionMap, Set<Suggestion> suggestions) {
        for (Suggestion suggestion : suggestions) {
            suggestionMap.putIfAbsent(suggestion.getName() + suggestion.getId(), suggestion);
        }
    }

    private void print(List<String> suggestionLines) {
        try (FileWriter fw = new FileWriter(outputFile);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            for (String suggestionLine : suggestionLines) {
                out.println(suggestionLine);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to create taxonomy suggestions file, " + outputFile, e);
        }
    }

    private Set<Suggestion> loadDefaultSynonyms() {
        InputStream inputStream = TaxonomySuggestions.class.getClassLoader()
                .getResourceAsStream(DEFAULT_TAXON_SYNONYMS_FILE);
        if (inputStream != null) {
            try (Stream<String> lines = new BufferedReader(new InputStreamReader(inputStream)).lines()) {
                return lines.map(this::createDefaultSuggestion)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
            }
        }
        return emptySet();
    }

    private Set<Suggestion> loadFromDB() throws SQLException {
        DbConnectionInfo dbConnectionInfo = DbConnInfos.getConnection(taxonomyConnectionStr);
        AtomicInteger counter = new AtomicInteger();

        Set<Suggestion> suggestions = new HashSet<>();
        try (Connection conn = dbConnectionInfo.createConnection();
             Statement statement = conn.createStatement();
             ResultSet resultSet = statement.executeQuery(ALL_TAX_QUERY)) {
            while (resultSet.next()) {
                TaxEntity taxEntity = convertRecord(resultSet);
                createTaxSuggestions(taxEntity)//.stream()
                        .forEach(suggestion -> {
                            int currentCount = counter.getAndIncrement();
                            if (currentCount % STATS_REPORT_CHUNK_SIZE == 0) {
                                LOGGER.info("Added {} taxonomy suggestions.", currentCount);
                            }
                            suggestions.add(suggestion);
                        });
            }
        }
        return suggestions;
    }

    private Set<Suggestion> createSuggestions(Integer taxId, String scientific, String common, String synonym) {
        Set<Suggestion> suggestions = new HashSet<>();
        if (scientific != null) {
            if (common != null) {
                suggestions.add(createSuggestion(taxId, scientific, common));
            }
            if (synonym != null) {
                suggestions.add(createSuggestion(taxId, scientific, synonym));
            }
            if (suggestions.isEmpty()) {
                suggestions.add(createSuggestion(taxId, scientific));
            }

        } else {
            if (common != null) {
                suggestions.add(createSuggestion(taxId, common));
            }
            if (synonym != null) {
                suggestions.add(createSuggestion(taxId, synonym));
            }
        }
        return suggestions;
    }

    private Suggestion createSuggestion(Integer taxId, String scientific, String alternativeName) {
        String name = createName(scientific, alternativeName);
        return Suggestion.builder()
                .id(taxId.toString())
                .name(name)
                .weight(computeWeightForName(name))
                .build();
    }

    private Suggestion createSuggestion(Integer taxId, String scientific) {
        return Suggestion.builder()
                .id(taxId.toString())
                .name(scientific)
                .weight(computeWeightForName(scientific))
                .build();
    }

    private Suggestion createDefaultSuggestion(String csvLine) {
        String[] lineParts = csvLine.split(",");
        if (!csvLine.startsWith(COMMENT_LINE_PREFIX) && lineParts.length == 3) {
            return Suggestion.builder()
                    .name(createName(lineParts[0], lineParts[2]))
                    .id(lineParts[1])
                    .weight(100)
                    .build();
        } else {
            return null;
        }
    }

    private String createName(String scientific, String alternativeName) {
        return alternativeName + NAME_DELIMITER + scientific;
    }

    private TaxEntity convertRecord(ResultSet resultSet) throws SQLException {
        return TaxEntity.builder()
                .taxId(resultSet.getInt("tax_id"))
                .sptrScientific(resultSet.getString("sptr_scientific"))
                .sptrCommon(resultSet.getString("sptr_common"))
                .sptrSynonym(resultSet.getString("sptr_synonym"))
                .ncbiCommon(resultSet.getString("ncbi_common"))
                .ncbiScientific(resultSet.getString("ncbi_scientific"))
                .build();
    }

    @Builder
    @Data
    static class TaxEntity {
        private Integer taxId;
        private String sptrScientific;
        private String sptrCommon;
        private String sptrSynonym;
        private String ncbiScientific;
        private String ncbiCommon;
    }
}
