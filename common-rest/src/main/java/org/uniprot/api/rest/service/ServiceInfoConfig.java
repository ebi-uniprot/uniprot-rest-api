package org.uniprot.api.rest.service;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Created 31/03/20
 *
 * @author Edd
 */
@Slf4j
@Configuration
public class ServiceInfoConfig {
    @Value("${serviceInfoPath}")
    private Resource serviceInfoPath;

    @Bean
    @SuppressWarnings("unchecked")
    public ServiceInfo serviceInfo() {
        try {
            Map<String, Object> serviceInfoMap;
            if (serviceInfoPath.exists()) {
                serviceInfoMap = new ObjectMapper().readValue(serviceInfoPath.getFile(), Map.class);
            } else {
                serviceInfoMap = Collections.emptyMap();
            }

            ServiceInfo serviceInfo = ServiceInfo.builder().map(serviceInfoMap).build();
            serviceInfo.validate();
            return serviceInfo;
        } catch (IOException e) {
            log.error("Error adding actuator service information", e);
            throw new IllegalStateException("Could not read service information", e);
        }
    }

    @Getter
    @Builder
    public static class ServiceInfo {
        static final String RELEASE = "release";
        private Map<String, Object> map;

        void validate() {
            if (!map.containsKey(RELEASE)) {
                throw new IllegalStateException(
                        "Service information must contain a 'release' key. Please define it.");
            }
        }

        public String getRelease() {
            return map.get(RELEASE).toString();
        }
    }
}
