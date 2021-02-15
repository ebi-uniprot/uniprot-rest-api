package org.uniprot.api.idmapping.service;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Created 15/02/2021
 *
 * @author Edd
 */
@Configuration
public class IDMappingConfig {
    @Bean
    public RestTemplate idMappingRestTemplate() {
        return new RestTemplateBuilder().build();
    }
}
