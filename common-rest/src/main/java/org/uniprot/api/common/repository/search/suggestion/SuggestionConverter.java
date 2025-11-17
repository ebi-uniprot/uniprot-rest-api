package org.uniprot.api.common.repository.search.suggestion;

import static java.util.Collections.emptyList;
import static org.apache.solr.client.solrj.response.SpellCheckResponse.Collation;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SpellCheckResponse;
import org.apache.solr.common.util.NamedList;
import org.springframework.core.convert.converter.Converter;
import org.uniprot.core.util.Utils;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.field.validator.FieldRegexConstants;

/**
 * Converts a {@link QueryResponse}'s spellcheck component to a list of {@link Suggestion}s. Created
 *
 * <p>29/07/21
 *
 * @author Edd
 */
public class SuggestionConverter implements Converter<QueryResponse, List<Suggestion>> {

    private SolrCollection collection;

    public SuggestionConverter(SolrCollection collection) {
        this.collection = collection;
    }

    @Override
    public List<Suggestion> convert(QueryResponse queryResponse) {
        long resultHits = queryResponse.getResults().getNumFound();
        SpellCheckResponse spellCheckResponse = queryResponse.getSpellCheckResponse();
        if (resultHits == 0L && Utils.notNull(spellCheckResponse)) {
            // Case 1: Solr already returned collations
            if (Utils.notNullNotEmpty(spellCheckResponse.getCollatedResults())) {
                return spellCheckResponse.getCollatedResults().stream()
                        .map(this::getSuggestion)
                        .toList();
            }

            // Case 2: Build client-side collations from token suggestions
            if (Utils.notNullNotEmpty(spellCheckResponse.getSuggestions())) {
                String query =
                        (String) ((NamedList<?>) queryResponse.getHeader().get("params")).get("q");
                if (!StringUtils.containsWhitespace(query)) {
                    return spellCheckResponse.getSuggestions().stream()
                            .flatMap(
                                    sug ->
                                            sug.getAlternatives().stream()
                                                    .map(alt -> getCollation(sug, alt, query)))
                            .map(this::getSuggestion)
                            .toList();
                }
            } else if (this.collection == SolrCollection.uniref) {
                String query =
                        (String) ((NamedList<?>) queryResponse.getHeader().get("params")).get("q");
                if (query.matches(FieldRegexConstants.UNIREF_CLUSTER_ID_REGEX)) {
                    List<Suggestion> suggestions = getUniRefSolrSuggestion(query);
                    return suggestions;
                }
            }
        }

        return emptyList();
    }

    private static Collation getCollation(
            SpellCheckResponse.Suggestion suggestion, String alternative, String query) {
        String collationQuery = query.toLowerCase().replace(suggestion.getToken(), alternative);
        return new Collation().setCollationQueryString(collationQuery);
    }

    private Suggestion getSuggestion(Collation collation) {
        return Suggestion.builder()
                .query(collation.getCollationQueryString().replace(" AND ", " "))
                .hits(collation.getNumberOfHits())
                .build();
    }

    private List<Suggestion> getUniRefSolrSuggestion(String query) {
        try {
            String[] tokens = query.split("_");
            String uniRefPrefix = tokens[0];
            String identityQuery = getIdentityQuery(uniRefPrefix);
            String uniRefSuffix = tokens[1];
            String idQuery = getUniProtKBOrUniParcIdQuery(uniRefSuffix);
            String suggestionQuery = identityQuery + " AND " + idQuery;
            return List.of(Suggestion.builder().query(suggestionQuery).build());
        } catch (IllegalArgumentException e) {
            // do nothing
        }
        return emptyList();
    }

    private String getIdentityQuery(String uniRefPrefix) {
        // take out digits after "UniRef"
        String numberStr = uniRefPrefix.substring("UniRef".length());
        int value = Integer.parseInt(numberStr);
        double identity;
        switch (value) {
            case 100:
                identity = 1.0;
                break;
            case 90:
                identity = 0.9;
                break;
            case 50:
                identity = 0.5;
                break;
            default:
                throw new IllegalArgumentException("Invalid UniRef identity: " + uniRefPrefix);
        }
        return "(identity:" + identity + ")";
    }

    private String getUniProtKBOrUniParcIdQuery(String uniRefSuffix) {
        if (uniRefSuffix.matches(FieldRegexConstants.UNIPROTKB_ACCESSION_REGEX)) {
            return "(uniprotkb:" + uniRefSuffix + ")";
        } else if (uniRefSuffix.matches(FieldRegexConstants.UNIPARC_UPI_REGEX)) {
            return "(uniparc:" + uniRefSuffix + ")";
        }
        throw new IllegalArgumentException("Expected UniProtKB or UniParc Id: " + uniRefSuffix);
    }
}
