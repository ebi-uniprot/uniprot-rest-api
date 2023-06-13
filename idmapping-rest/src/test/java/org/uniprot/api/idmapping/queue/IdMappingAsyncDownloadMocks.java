package org.uniprot.api.idmapping.queue;

import static org.mockito.Mockito.mock;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

@Profile({"offline & !asyncDownload"})
@TestConfiguration
public class IdMappingAsyncDownloadMocks {

    @Bean
    public IdMappingProducerMessageService idMappingProducerMessageService() {
        return mock(IdMappingProducerMessageService.class);
    }
}
