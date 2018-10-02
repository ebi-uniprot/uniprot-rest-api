package uk.ac.ebi.uniprot.uuw.suggester;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import lombok.Builder;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.kraken.database.util.DbConnInfos;
import uk.ac.ebi.kraken.database.util.DbConnectionInfo;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Created 28/09/18
 *
 * @author Edd
 */
public class TaxonomySuggestions {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaxonomySuggestions.class);
    private static final int STATS_REPORT_CHUNK_SIZE = 100000;
    private static final String ALL_TAX_QUERY = "select t.tax_id, t.ncbi_scientific, t.ncbi_common," +
            "t.sptr_scientific,t.sptr_common,t.sptr_synonym" +
            " from taxonomy.v_public_node t";

    @Parameter(names = {"--output-file", "-o"}, description = "The destination file")
    private String outputFile = "taxonSuggestions.txt";

    @Parameter(names = {"--tax-connection", "-c"}, description = "The connection details to the taxonomy DB", required = true)
    private String taxonomyConnectionStr;

    public static void main(String[] args) throws SQLException {
        TaxonomySuggestions suggestions = new TaxonomySuggestions();
        JCommander.newBuilder()
                .addObject(suggestions)
                .build()
                .parse(args);
        suggestions.createFile();
    }

    private void createFile() throws SQLException {
        DbConnectionInfo dbConnectionInfo = DbConnInfos.getConnection(taxonomyConnectionStr);
        AtomicInteger counter = new AtomicInteger();

        try (Connection conn = dbConnectionInfo.createConnection();
             Statement statement = conn.createStatement();
             ResultSet resultSet = statement.executeQuery(ALL_TAX_QUERY);
             FileWriter fw = new FileWriter(outputFile, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            while (resultSet.next()) {
                TaxRow taxRow = convertRecord(resultSet);
                createTaxSuggestions(taxRow).stream()
                        .map(Suggestion::toSuggestionLine)
                        .forEach(suggestion -> {
                            int currentCount = counter.getAndIncrement();
                            if (currentCount % STATS_REPORT_CHUNK_SIZE == 0) {
                                LOGGER.info("Added {} taxonomy suggestions.", currentCount);
                            }
                            out.println(suggestion);
                        });
            }
        } catch (IOException e) {
            LOGGER.error("Failed to create taxonomy suggestions file, " + taxonomyConnectionStr, e);
        }
    }

    private List<Suggestion> createTaxSuggestions(TaxRow taxRow) {
        List<Suggestion> suggestions = new ArrayList<>();

        // scientific name
        Suggestion scientificSuggestion = createSuggestion(taxRow, TaxRow::getSptrScientific);
        if (Objects.isNull(scientificSuggestion)) {
            scientificSuggestion = createSuggestion(taxRow, TaxRow::getNcbiScientific);
        }
        if (Objects.nonNull(scientificSuggestion)) {
            suggestions.add(scientificSuggestion);
        }

        // common name
        Suggestion commonSuggestion = createSuggestion(taxRow, TaxRow::getSptrCommon);
        if (Objects.isNull(commonSuggestion)) {
            commonSuggestion = createSuggestion(taxRow, TaxRow::getNcbiCommon);
        }
        if (Objects.nonNull(commonSuggestion)) {
            suggestions.add(commonSuggestion);
        }

        // synonym
        Suggestion synonymSuggestion = createSuggestion(taxRow, TaxRow::getSptrSynonym);
        if (Objects.nonNull(synonymSuggestion)) {
            suggestions.add(synonymSuggestion);
        }

        return suggestions;
    }

    private Suggestion createSuggestion(TaxRow taxRow, Function<TaxRow, String> nameGetter) {
        Suggestion suggestion = null;
        if (Objects.nonNull(nameGetter.apply(taxRow))) {
            suggestion = Suggestion.builder()
                    .name(nameGetter.apply(taxRow))
                    .id(taxRow.getTaxId().toString())
                    .build();
        }

        return suggestion;
    }

    private TaxRow convertRecord(ResultSet resultSet) throws SQLException {
        return TaxRow.builder()
                .taxId(resultSet.getInt("tax_id"))
                .sptrScientific(resultSet.getString("sptr_scientific"))
                .sptrCommon(resultSet.getString("sptr_common"))
                .sptrSynonym(resultSet.getString("sptr_synonym"))
                .ncbiCommon(resultSet.getString("ncbi_common"))
                .ncbiScientific(resultSet.getString("ncbi_scientific"))
                .build();
    }

    @Builder
    private static class Suggestion {
        String id;
        String name;

        String toSuggestionLine() {
            return name + " [" + id + "]";
        }
    }

    @Builder
    @Data
    private static class TaxRow {
        private Integer taxId;
        private String sptrScientific;
        private String sptrCommon;
        private String sptrSynonym;
        private String ncbiScientific;
        private String ncbiCommon;
    }
}
