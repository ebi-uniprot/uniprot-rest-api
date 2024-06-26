package org.uniprot.api.async.download.messaging.consumer;

import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.uniprot.api.async.download.common.AbstractDownloadIT;
import org.uniprot.api.async.download.messaging.config.common.MessageProducerConfig;
import org.uniprot.api.async.download.messaging.config.common.RabbitMQConfigs;
import org.uniprot.api.async.download.messaging.config.common.RedisConfiguration;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.HeartbeatConfig;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import({MessageProducerConfig.class, RedisConfiguration.class, RabbitMQConfigs.class})
@EnableConfigurationProperties({HeartbeatConfig.class})
@TestPropertySource("classpath:application.properties")
public abstract class BasicMessageConsumerIT extends AbstractDownloadIT {}
