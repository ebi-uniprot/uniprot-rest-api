package org.uniprot.api.uniprotkb.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.service.ServiceInfoConfig;

/**
 * This class is responsible for adding descriptive service information to the Spring Actuator
 * 'info' end-point. It is configured to load its contents from a file, see {@link
 * ServiceInfoConfig}. This file must contain a 'release' key and value.
 *
 * <p>Created 31/03/20
 *
 * @author Edd
 */
@Component
@Import(ServiceInfoConfig.class)
public class UniProtKBInfoContributor implements InfoContributor {
    static final String SERVICE_INFO_KEY = "service-info";
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
