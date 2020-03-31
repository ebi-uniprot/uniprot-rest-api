package org.uniprot.api.uniprotkb.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.service.ServiceInfoConfig;

/**
 * Created 31/03/20
 *
 * @author Edd
 */
@Component
@Import(ServiceInfoConfig.class)
public class UniProtKBInfoContributor implements InfoContributor {
    private static final String SERVICE_INFO_KEY = "service-info";
    private final ServiceInfoConfig.ServiceInfo serviceInfo;

    @Autowired
    public UniProtKBInfoContributor(ServiceInfoConfig.ServiceInfo serviceInfo) {
        this.serviceInfo = serviceInfo;
    }

    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail(SERVICE_INFO_KEY, serviceInfo.getMap());
    }
}
