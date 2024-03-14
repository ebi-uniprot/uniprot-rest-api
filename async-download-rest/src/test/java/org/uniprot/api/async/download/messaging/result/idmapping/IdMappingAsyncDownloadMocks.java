package org.uniprot.api.async.download.messaging.result.idmapping;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;

@Profile({"offline & !asyncDownload"})
@TestConfiguration
public class IdMappingAsyncDownloadMocks {

    //    @Bean
    //    public IdMappingProducerMessageService idMappingProducerMessageService() {
    //        return mock(IdMappingProducerMessageService.class);
    //    }
}
