package org.uniprot.api.common.repository.search;

import java.util.Map;

/**
 * Created 10/09/19
 *
 * @author Edd
 */
public interface QueryBoostsConfig {
    Map<String, String> getDefaultBoostsMap();
    String getDefaultBoostFunction();
    Map<String, String> getAdvancedBoostsMap();
    String getAdvancedBoostFunctions();
}
