package org.uniprot.api.rest.download;

import org.springframework.amqp.core.MessageListener;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.uniprot.api.rest.download.queue.ProducerMessageService;
import org.uniprot.api.rest.download.repository.DownloadJobRepository;

import static org.mockito.Mockito.mock;

@Profile({"offline & !asyncDownload", "use-fake-app", "server-errors", "viewbyTest"})
@TestConfiguration
public class AsyncDownloadMocks {
    @Bean
    public ProducerMessageService producerMessageService() {
        return mock(ProducerMessageService.class);
    }

    @Bean
    public DownloadJobRepository downloadJobRepository() {
        return mock(DownloadJobRepository.class);
    }

    @Bean
    public MessageListener messageListener() {
        return mock(MessageListener.class);
    }

    @Bean
    public RedisTemplate redisTemplate() {
        return mock(RedisTemplate.class);
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate() {
        return mock(StringRedisTemplate.class);
    }
}
