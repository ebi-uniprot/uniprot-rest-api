package org.uniprot.api.async.download.refactor.producer.uniprotkb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.async.download.messaging.config.common.MessageProducerConfig;
import org.uniprot.api.async.download.messaging.config.common.RabbitMQConfigs;
import org.uniprot.api.async.download.messaging.config.common.RedisConfiguration;
import org.uniprot.api.async.download.messaging.config.uniprotkb.UniProtKBDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.config.uniprotkb.UniProtKBRabbitMQConfig;
import org.uniprot.api.async.download.messaging.listener.common.HeartbeatConfig;
import org.uniprot.api.async.download.messaging.listener.uniprotkb.UniProtKBMessageListener;
import org.uniprot.api.async.download.messaging.result.uniprotkb.UniProtKBDownloadResultWriter;
import org.uniprot.api.async.download.refactor.messaging.uniprotkb.UniProtKBMessagingService;
import org.uniprot.api.async.download.refactor.request.uniprotkb.UniProtKBDownloadRequest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension.class)
@Import({UniProtKBProducerTestConfig.class, UniProtKBRabbitMQConfig.class, MessageProducerConfig.class, RedisConfiguration.class, RabbitMQConfigs.class})
@EnableConfigurationProperties({HeartbeatConfig.class, UniProtKBDownloadConfigProperties.class})
@TestPropertySource("classpath:application.properties")
class UniProtKBProducerMessageServiceIT {

    @Autowired
    private UniProtKBProducerMessageService service;

    @MockBean
    private UniProtKBMessagingService messageService;

    @MockBean
    private UniProtKBDownloadResultWriter writer;

    @MockBean
    private UniProtKBMessageListener listener;

    @Captor
    ArgumentCaptor<Message> messageCaptor;

    @Test
    void sendMessage_withForceAndAllowed() {
        UniProtKBDownloadRequest request = new UniProtKBDownloadRequest();
        request.setQuery("query value");
        request.setSort("accession asc");
        request.setFormat("json");
        request.setFields("accession,gene");
        String jobId = service.sendMessage(request);
        assertNotNull(jobId);
        assertEquals("60ba2e259320dcb5a23f2e432c8f6bc6d8ed417f", jobId);

        Mockito.verify(messageService).send(messageCaptor.capture());
        Message message = messageCaptor.getValue();
        assertNotNull(message);
        assertNotNull(message.getMessageProperties());
        MessageProperties messageValues = message.getMessageProperties();
        assertEquals("application/json", messageValues.getContentType());
        assertEquals("UTF-8", messageValues.getContentEncoding());
        Map<String, Object> headers = messageValues.getHeaders();
        assertNotNull(headers);
        assertNotNull(message.getBody());
        //REdis part check
        //Message Header
        //
    }



}
