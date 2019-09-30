package org.uniprot.api.common.repository.search.term;

import static java.util.Collections.emptyList;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.TermsResponse;
import org.springframework.core.convert.converter.Converter;
import org.uniprot.core.util.Utils;

/**
 * Created 14/06/19
 *
 * @author Edd
 */
public class TermInfoConverter implements Converter<QueryResponse, List<TermInfo>> {

    @Override
    public List<TermInfo> convert(QueryResponse queryResponse) {
        TermsResponse termsResponse = queryResponse.getTermsResponse();

        if (Utils.nonNull(termsResponse)) {
            return termsResponse.getTermMap().entrySet().stream()
                    .map(this::termEntryToTermInfo)
                    .filter(Utils::nonNull)
                    .collect(Collectors.toList());
        } else {
            return emptyList();
        }
    }

    private TermInfo termEntryToTermInfo(Map.Entry<String, List<TermsResponse.Term>> entry) {
        TermInfo.TermInfoBuilder builder = TermInfo.builder();
        builder.name(entry.getKey());

        if (!entry.getValue().isEmpty()) {
            builder.hits(entry.getValue().get(0).getFrequency());
            return builder.build();
        } else {
            return null;
        }
    }
}
