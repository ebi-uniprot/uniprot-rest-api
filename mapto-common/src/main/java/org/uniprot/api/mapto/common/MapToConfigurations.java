package org.uniprot.api.mapto.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.api.mapto.common.service.MapToHashGenerator;

@Configuration
public class MapToConfigurations {

    private static final String SALT_STR = "MAP_TO_SALT";

    @Bean
    public MapToHashGenerator mapToHashGenerator() {
        return new MapToHashGenerator(request -> {
            String builder = request.getSource().name().toLowerCase() +
                    request.getTarget().name().toLowerCase() +
                    request.getQuery().strip().toLowerCase();
            return builder.toCharArray();
        }, SALT_STR);
    }
}
