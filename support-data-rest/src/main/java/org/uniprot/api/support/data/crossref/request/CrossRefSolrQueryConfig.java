package org.uniprot.api.support.data.crossref.request;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrQueryConfigFileReader;
import org.uniprot.api.rest.service.query.QueryProcessor;
import org.uniprot.api.rest.service.query.UniProtQueryProcessor;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
import org.uniprot.api.rest.validation.config.WhitelistFieldConfig;
import org.uniprot.api.support.data.crossref.service.CrossRefService;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;

@Configuration
public class CrossRefSolrQueryConfig {
    private static final String RESOURCE_LOCATION = "/crossref-query.config";

    @Bean
    public SolrQueryConfig crossRefSolrQueryConf() {
        return new SolrQueryConfigFileReader(RESOURCE_LOCATION).getConfig();
    }

    @Bean
    public SearchFieldConfig crossRefSearchFieldConfig() {
        return SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.CROSSREF);
    }

    @Bean
    public QueryProcessor crossRefQueryProcessor(
            WhitelistFieldConfig whiteListFieldConfig,
            SearchFieldConfig crossRefSearchFieldConfig) {
        Map<String, String> crossRefWhiteListFields =
                whiteListFieldConfig
                        .getField()
                        .getOrDefault(
                                UniProtDataType.CROSSREF.toString().toLowerCase(), new HashMap<>());
        return UniProtQueryProcessor.newInstance(
                UniProtQueryProcessorConfig.builder()
                        .optimisableFields(
                                getDefaultSearchOptimisedFieldItems(crossRefSearchFieldConfig))
                        .whiteListFields(crossRefWhiteListFields)
                        .build());
    }

    private List<SearchFieldItem> getDefaultSearchOptimisedFieldItems(
            SearchFieldConfig crossRefSearchFieldConfig) {
        return Collections.singletonList(
                crossRefSearchFieldConfig.getSearchFieldItemByName(
                        CrossRefService.CROSS_REF_ID_FIELD));
    }
}
