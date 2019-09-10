package org.uniprot.api.uniprotkb.repository.search.impl;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.uniprot.api.common.repository.search.QueryBoostsConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * Created 10/09/19
 *
 * @author Edd
 */
@Component
@Getter
@Setter
@PropertySource("classpath:uniprotkb-query-boosts.properties")
@ConfigurationProperties(prefix = "boost.uniprot")
public class UniProtQueryBoostsConfig2 implements QueryBoostsConfig {

    private Map<String, String> defaultBoosts = new HashMap<>();
    private String defaultBoostFunctions;
    private Map<String, String> advancedBoosts = new HashMap<>();
    private String advancedBoostFunctions;

    @Override
    public Map<String, String> getDefaultBoostsMap() {
        return defaultBoosts;
    }

    @Override
    public String getDefaultBoostFunctions() {
        return defaultBoostFunctions;
    }

    @Override
    public Map<String, String> getAdvancedBoostsMap() {
        return advancedBoosts;
    }

    @Override
    public String getAdvancedBoostFunctions() {
        return advancedBoostFunctions;
    }
}
