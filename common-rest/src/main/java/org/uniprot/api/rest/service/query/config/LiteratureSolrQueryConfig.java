<<<<<<< HEAD:support-data-rest/src/main/java/org/uniprot/api/support/data/literature/request/LiteratureSolrQueryConfig.java
package org.uniprot.api.support.data.literature.request;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
=======
package org.uniprot.api.rest.service.query.config;
>>>>>>> move LiteratureSolrQueryConfig; add "resettable" sort clause, so that score need not be used by default:common-rest/src/main/java/org/uniprot/api/rest/service/query/config/LiteratureSolrQueryConfig.java

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrQueryConfigFileReader;
import org.uniprot.api.rest.service.query.QueryProcessor;
import org.uniprot.api.rest.service.query.UniProtQueryProcessor;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
<<<<<<< HEAD:support-data-rest/src/main/java/org/uniprot/api/support/data/literature/request/LiteratureSolrQueryConfig.java
import org.uniprot.api.rest.validation.config.WhitelistFieldConfig;
import org.uniprot.api.support.data.literature.service.LiteratureService;
=======
>>>>>>> move LiteratureSolrQueryConfig; add "resettable" sort clause, so that score need not be used by default:common-rest/src/main/java/org/uniprot/api/rest/service/query/config/LiteratureSolrQueryConfig.java
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;

import java.util.Collections;
import java.util.List;

import static java.util.Collections.emptyMap;

@Configuration
public class LiteratureSolrQueryConfig {
    private static final String RESOURCE_LOCATION = "/literature-query.config";

    @Bean
    public SolrQueryConfig literatureSolrQueryConf() {
        return new SolrQueryConfigFileReader(RESOURCE_LOCATION).getConfig();
    }

    @Bean
    public SearchFieldConfig literatureSearchFieldConfig() {
        return SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.LITERATURE);
    }

    @Bean
    public QueryProcessor literatureQueryProcessor(SearchFieldConfig literatureSearchFieldConfig) {
        return UniProtQueryProcessor.newInstance(
                UniProtQueryProcessorConfig.builder()
                        .optimisableFields(
                                getDefaultSearchOptimisedFieldItems(literatureSearchFieldConfig))
                        .whiteListFields(emptyMap())
                        .build());
    }

    private List<SearchFieldItem> getDefaultSearchOptimisedFieldItems(
            SearchFieldConfig literatureSearchFieldConfig) {
        return Collections.singletonList(
                literatureSearchFieldConfig.getSearchFieldItemByName("id"));
    }
}
