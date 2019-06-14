package uk.ac.ebi.uniprot.api.common.repository.search.term;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.TermsResponse;
import org.springframework.core.convert.converter.Converter;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created 14/06/19
 *
 * @author Edd
 */
public class TermInfoConverter implements Converter<QueryResponse, List<TermInfo>> {

    @Override
    public List<TermInfo> convert(QueryResponse queryResponse) {
        TermsResponse termsResponse = queryResponse.getTermsResponse();

        return termsResponse.getTermMap()
                .entrySet()
                .stream()
                .map(termEntryToTermInfo())
                .collect(Collectors.toList());
    }

    private Function<Map.Entry<String, List<TermsResponse.Term>>, TermInfo> termEntryToTermInfo() {
        return entry -> {
            TermInfo.TermInfoBuilder builder = TermInfo.builder();
            builder.name(entry.getKey());

            // TODO: 14/06/19 need to add url, and get correct url.

            if (!entry.getValue().isEmpty()) {
                builder.hits(entry.getValue().get(0).getFrequency());
            }
            
            return builder.build();};
    }
}
