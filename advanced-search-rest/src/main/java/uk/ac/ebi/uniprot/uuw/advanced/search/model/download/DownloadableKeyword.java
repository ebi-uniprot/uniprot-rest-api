package uk.ac.ebi.uniprot.uuw.advanced.search.model.download;

import com.google.common.base.Strings;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.Keyword;

import java.util.*;
import java.util.stream.Collectors;

public class DownloadableKeyword implements Downloadable {
    private final List<Keyword> keywords;
    public static final List<String> FIELDS =
            Arrays.asList(
                    "keyword", "keywordid"
            );

    public DownloadableKeyword(List<Keyword> keywords) {
        if (keywords == null) {
            this.keywords = Collections.emptyList();
        } else {
            this.keywords = Collections.unmodifiableList(keywords);
        }
    }

    @Override
    public Map<String, String> attributeValues() {
        if (keywords.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> map = new HashMap<>();
        String kwValue =
                keywords.stream().map(val -> val.getValue().getValue()).collect(Collectors.joining(";"));
        map.put(FIELDS.get(0), kwValue);
        String kwIds =
                keywords.stream().map(Keyword::getKeywordId).filter(val -> !Strings.isNullOrEmpty(val))
                        .collect(Collectors.joining("; "));
        map.put(FIELDS.get(1), kwIds);
        return map;
    }

    public static boolean contains(List<String> fields) {
        return fields.stream().anyMatch(FIELDS::contains);
    }

}
