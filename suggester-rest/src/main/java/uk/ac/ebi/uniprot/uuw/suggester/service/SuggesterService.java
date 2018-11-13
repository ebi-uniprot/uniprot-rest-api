package uk.ac.ebi.uniprot.uuw.suggester.service;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.uniprot.uuw.suggester.SuggestionDictionary;
import uk.ac.ebi.uniprot.uuw.suggester.model.Suggestions;

import java.io.IOException;
import java.util.stream.Collectors;

/**
 * Created 18/07/18
 *
 * @author Edd
 */
public class SuggesterService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SuggesterService.class);
    private static final String SUGGEST_HANDLER = "/suggest";
    private static final String SUGGEST_DICTIONARY = "suggest.dictionary";
    private static final String SUGGEST_Q = "suggest.q";
    private final SolrClient solrClient;

    public SuggesterService(SolrClient solrClient) {
        this.solrClient = solrClient;
    }

    public Suggestions getSuggestions(SuggestionDictionary dictionary, String query) {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setRequestHandler(SUGGEST_HANDLER);
        solrQuery.add(SUGGEST_DICTIONARY, dictionary.getId());
        solrQuery.add(SUGGEST_Q, query);

        try {
            return Suggestions.createSuggestions(
                    dictionary,
                    query,
                    solrClient.query(solrQuery)
                            .getSuggesterResponse()
                            .getSuggestedTerms()
                            .get(dictionary.getId())
                            .stream()
                            .distinct()
                            .collect(Collectors.toList()));
        } catch (SolrServerException | IOException e) {
            String message = "Could not get suggestions for: [" + dictionary.getId() + ", " + query + "]";
            LOGGER.error(message, e);
            throw new SuggestionRetrievalException(message);
        }
    }
}
