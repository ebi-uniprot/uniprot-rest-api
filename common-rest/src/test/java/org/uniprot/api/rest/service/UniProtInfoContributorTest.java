package org.uniprot.api.rest.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.uniprot.api.rest.service.UniProtInfoContributor.SERVICE_INFO_KEY;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.info.Info;

/**
 * Created 10/07/2020
 *
 * @author Edd
 */
class UniProtInfoContributorTest {
    @Test
    void infoBuilderIsUpdated() {
        Map<String, Object> serviceInfoMap = new HashMap<>();
        serviceInfoMap.put("release", "releaseValue");
        ServiceInfoConfig.ServiceInfo serviceInfo =
                ServiceInfoConfig.ServiceInfo.builder().map(serviceInfoMap).build();
        UniProtInfoContributor uniProtKBInfoContributor = new UniProtInfoContributor(serviceInfo);
        Info.Builder infoBuilder = new Info.Builder();

        uniProtKBInfoContributor.contribute(infoBuilder);

        Map<String, Object> details = infoBuilder.build().getDetails();
        assertThat(details.containsKey(SERVICE_INFO_KEY), is(true));
        assertThat(details.get(SERVICE_INFO_KEY), is(serviceInfoMap));
    }
}
