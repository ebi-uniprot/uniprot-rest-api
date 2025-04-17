package org.uniprot.api.mapto.common;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.api.mapto.common.service.MapToHashGenerator;

import net.jodah.failsafe.RetryPolicy;

@Configuration
public class MapToConfigurations {

    private static final String SALT_STR = "MAP_TO_SALT";
    // todo define retry configs

    @Bean
    public MapToHashGenerator mapToHashGenerator() {
        return new MapToHashGenerator(
                request -> {
                    String builder =
                            request.getSource().name().toLowerCase()
                                    + request.getTarget().name().toLowerCase()
                                    + request.getQuery().strip().toLowerCase();
                    return builder.toCharArray();
                },
                SALT_STR);
    }

    @Bean
    public RetryPolicy<Object> retryPolicy() {
        return new RetryPolicy<>()
                .handle(Exception.class)
                .withMaxRetries(3)
                .withDelay(Duration.ofMillis(20));
    }
}
