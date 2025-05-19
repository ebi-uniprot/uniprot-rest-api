package org.uniprot.api.mapto.common;

import java.time.Duration;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.api.mapto.common.model.MapToJobRequest;
import org.uniprot.api.mapto.common.service.MapToHashGenerator;

import net.jodah.failsafe.RetryPolicy;

@Configuration
public class MapToConfigurations {

    private static final String SALT_STR = "MAP_TO_SALT";
    // todo define retry configs

    @Bean
    public MapToHashGenerator mapToHashGenerator() {
        return new MapToHashGenerator(this::convertRequestToArray, SALT_STR);
    }

    private char[] convertRequestToArray(MapToJobRequest request) {
        // write code to create key and value concataned string
        String extraParams =
                request.getExtraParams().entrySet().stream()
                        .map(entry -> entry.getKey() + entry.getValue())
                        .collect(Collectors.joining(","));
        String concatenatedStrings =
                request.getSource().name().toLowerCase()
                        + request.getTarget().name().toLowerCase()
                        + request.getQuery().strip().toLowerCase()
                        + extraParams;
        return concatenatedStrings.toString().toCharArray();
    }

    @Bean
    public RetryPolicy<Object> retryPolicy() {
        return new RetryPolicy<>()
                .handle(Exception.class)
                .withMaxRetries(3)
                .withDelay(Duration.ofMillis(20));
    }
}
