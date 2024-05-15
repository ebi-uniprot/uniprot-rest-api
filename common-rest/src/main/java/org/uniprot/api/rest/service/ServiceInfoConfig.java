package org.uniprot.api.rest.service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Created 31/03/20
 *
 * @author Edd
 */
@Slf4j
@Configuration
public class ServiceInfoConfig {
    private static final String DEFAULT_NON_CACHEABLE_PATHS_CSV = ".*/idmapping/.*";

    @Value("${serviceInfoPath}")
    private Resource serviceInfoPath;

    @Value("${cache.control.max.age:#{3600}}")
    private Integer cacheControlMaxAge;

    @Value("${cache.ignore.paths:" + DEFAULT_NON_CACHEABLE_PATHS_CSV + "}")
    private List<String> nonCacheableEndPoints;

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

            ServiceInfo serviceInfo =
                    ServiceInfo.builder()
                            .map(serviceInfoMap)
                            .cacheControlMaxAge(cacheControlMaxAge)
                            .nonCacheablePaths(
                                    nonCacheableEndPoints.stream()
                                            .map(Pattern::compile)
                                            .collect(Collectors.toList()))
                            .build();
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
        static final String RELEASE_NUMBER = "releaseNumber";
        static final String RELEASE_DATE = "releaseDate";
        static final String DEPLOYMENT_DATE = "deploymentDate";
        static final String CACHE_CONTROL_MAX_AGE = "maxAgeInSeconds";
        private Map<String, Object> map;
        private Integer cacheControlMaxAge;
        private List<Pattern> nonCacheablePaths;

        void validate() {
            if (!map.containsKey(RELEASE_NUMBER)) {
                throw new IllegalStateException(
                        "Service information must contain a 'releaseNumber' key. Please define it.");
            }
            if (!map.containsKey(RELEASE_DATE)) {
                throw new IllegalStateException(
                        "Service information must contain a 'releaseDate' key. Please define it.");
            }
            if (!map.containsKey(DEPLOYMENT_DATE)) {
                throw new IllegalStateException(
                        "Service information must contain a 'deploymentDate' key. Please define it.");
            }
        }

        public String getReleaseNumber() {
            return map.get(RELEASE_NUMBER).toString();
        }

        public String getReleaseDate() {
            return map.get(RELEASE_DATE).toString();
        }

        public String getDeploymentDate() {
            return map.get(DEPLOYMENT_DATE).toString();
        }

        public Integer getMaxAgeInSeconds() {
            return this.cacheControlMaxAge;
        }

        public List<Pattern> getNonCacheablePaths() {
            return this.nonCacheablePaths;
        }
    }
}
