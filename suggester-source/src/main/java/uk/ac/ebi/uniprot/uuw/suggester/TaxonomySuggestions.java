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
 * Generates file used for taxonomy suggestions. Depends on a UniProt DB, e.g., SWPREAD, using data from
 * TAXONOMY.V_PUBLIC_NODE.
 *
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
    private String outputFile = "taxon-suggestions.txt";

    @Parameter(names = {"--tax-connection", "-c"}, description = "The connection details to the taxonomy DB", required = true)
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
        suggestions.createFile();
    }

    private void createFile() throws SQLException {
        DbConnectionInfo dbConnectionInfo = DbConnInfos.getConnection(taxonomyConnectionStr);
        AtomicInteger counter = new AtomicInteger();

        try (Connection conn = dbConnectionInfo.createConnection();
             Statement statement = conn.createStatement();
             ResultSet resultSet = statement.executeQuery(ALL_TAX_QUERY);
             FileWriter fw = new FileWriter(outputFile);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            while (resultSet.next()) {
                TaxEntity taxEntity = convertRecord(resultSet);
                createTaxSuggestions(taxEntity).stream()
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

    List<Suggestion> createTaxSuggestions(TaxEntity taxEntity) {
        List<Suggestion> suggestions = new ArrayList<>();

        // scientific name
        Suggestion scientificSuggestion = createSuggestion(taxEntity, TaxEntity::getSptrScientific);
        if (Objects.isNull(scientificSuggestion)) {
            scientificSuggestion = createSuggestion(taxEntity, TaxEntity::getNcbiScientific);
        }
        if (Objects.nonNull(scientificSuggestion)) {
            suggestions.add(scientificSuggestion);
        }

        // common name
        Suggestion commonSuggestion = createSuggestion(taxEntity, TaxEntity::getSptrCommon);
        if (Objects.isNull(commonSuggestion)) {
            commonSuggestion = createSuggestion(taxEntity, TaxEntity::getNcbiCommon);
        }
        if (Objects.nonNull(commonSuggestion)) {
            suggestions.add(commonSuggestion);
        }

        // synonym
        Suggestion synonymSuggestion = createSuggestion(taxEntity, TaxEntity::getSptrSynonym);
        if (Objects.nonNull(synonymSuggestion)) {
            suggestions.add(synonymSuggestion);
        }

        return suggestions;
    }

    private Suggestion createSuggestion(TaxEntity taxEntity, Function<TaxEntity, String> nameGetter) {
        Suggestion suggestion = null;
        if (Objects.nonNull(nameGetter.apply(taxEntity))) {
            suggestion = Suggestion.builder()
                    .name(nameGetter.apply(taxEntity))
                    .id(taxEntity.getTaxId().toString())
                    .build();
        }

        return suggestion;
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
