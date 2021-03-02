package org.uniprot.api.idmapping.service.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.idmapping.service.IdMappingPIRService;
import org.uniprot.api.idmapping.service.impl.PIRServiceImpl;

/**
 * Created 15/02/2021
 *
 * @author Edd
 */
@Configuration
public class IdMappingConfig {
    @Bean
    public RestTemplate idMappingRestTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder.build();
    }
}
