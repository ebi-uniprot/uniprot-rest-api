package uk.ac.ebi.uniprot.api.suggester;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.slf4j.Logger;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created 04/10/18
 *
 * @author Edd
 */
public class SuggesterManager {
    private static final Logger LOGGER = getLogger(SuggesterManager.class);

    @Parameter(names = {"--suggestion-type", "-t"}, description = "The type of suggestion file to create", required = true)
    private String suggestionFileType;

    public static void main(String[] args) throws IOException, SQLException {
        SuggesterManager suggestionHandler = new SuggesterManager();
        JCommander.newBuilder()
                .addObject(suggestionHandler)
                .acceptUnknownOptions(true)
                .build()
                .parse(args);

        String[] handlerArgs = excludeSuggestionTypeArg(args);

        SuggestionType suggestionType = SuggestionType.valueOf(suggestionHandler.suggestionFileType.toUpperCase());
        switch (suggestionType) {
            case EC:
                ECSuggestions.main(handlerArgs);
                break;
            case SUBCELL:
                SubCellSuggestions.main(handlerArgs);
                break;
            case TAXONOMY:
                TaxonomySuggestions.main(handlerArgs);
                break;
            case KEYWORD:
                KeywordSuggestions.main(handlerArgs);
                break;
            case GO:
                GOSuggestions.main(handlerArgs);
                break;
            case MAIN:
                MainSearchSuggestions.main(handlerArgs);
                break;
            default:
                LOGGER.warn("Unrecognised suggestion type. Expected one of: {}", asList(SuggestionType.values()));
        }
    }

    enum SuggestionType {
        EC, SUBCELL, TAXONOMY, KEYWORD, GO, MAIN
    }

    private static String[] excludeSuggestionTypeArg(String[] args) {
        List<String> handlerArgList = new ArrayList<>();
        for (int i = 0; i < args.length;) {
            String arg = args[i];
            if (arg.equals("-t") || arg.equals("--suggestion-type")) {
                i += 2;
                continue;
            }
            handlerArgList.add(arg);
            i++;
        }

        String[] handlerArgs = new String[handlerArgList.size()];
        return handlerArgList.toArray(handlerArgs);
    }
}
