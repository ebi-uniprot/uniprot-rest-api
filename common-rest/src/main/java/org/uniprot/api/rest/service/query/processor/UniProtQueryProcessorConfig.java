package org.uniprot.api.rest.service.query.processor;

import static java.util.Collections.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Builder;
import lombok.Getter;

import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;

/**
 * Used to store fields used to configure the query processor
 *
 * <p>Created 02/10/2020
 *
 * @author Edd
 */
@Getter
@Builder
public class UniProtQueryProcessorConfig {
    private final List<SearchFieldItem> optimisableFields;
    private final Map<String, String> whiteListFields;
    private final Set<String> stopwords;
    private final Set<String> leadingWildcardFields; // fields which support leading wildcard
    private final SearchFieldConfig searchFieldConfig;

    public static class UniProtQueryProcessorConfigBuilder {
        private List<SearchFieldItem> optimisableFields = emptyList();
        private Map<String, String> whiteListFields = emptyMap();
        private Set<String> stopwords = emptySet();
        private Set<String> searchFields = emptySet();
        private Set<String> leadingWildcardFields = emptySet();
    }
}
