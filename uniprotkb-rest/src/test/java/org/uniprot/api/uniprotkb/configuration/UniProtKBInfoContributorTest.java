package org.uniprot.api.uniprotkb.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.info.Info;
import org.uniprot.api.rest.service.ServiceInfoConfig;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.uniprot.api.uniprotkb.configuration.UniProtKBInfoContributor.SERVICE_INFO_KEY;

/**
 * Created 01/04/20
 *
 * @author Edd
 */
class UniProtKBInfoContributorTest {

    @Test
    void infoBuilderIsUpdated() {
        Map<String, Object> serviceInfoMap = new HashMap<>();
        serviceInfoMap.put("release", "releaseValue");
        ServiceInfoConfig.ServiceInfo serviceInfo =
                ServiceInfoConfig.ServiceInfo.builder().map(serviceInfoMap).build();
        UniProtKBInfoContributor uniProtKBInfoContributor =
                new UniProtKBInfoContributor(serviceInfo);
        Info.Builder infoBuilder = new Info.Builder();

        uniProtKBInfoContributor.contribute(infoBuilder);

        Map<String, Object> details = infoBuilder.build().getDetails();
        assertThat(details.containsKey(SERVICE_INFO_KEY), is(true));
        assertThat(details.get(SERVICE_INFO_KEY), is(serviceInfoMap));
    }
}
