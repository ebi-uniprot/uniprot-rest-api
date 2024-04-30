package org.uniprot.api.async.download.messaging.listener.uniprotkb.embeddings;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.TestInstance;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.uniprot.api.async.download.messaging.config.uniprotkb.embeddings.EmbeddingsQueueConfigProperties;
import org.uniprot.api.async.download.messaging.repository.UniProtKBDownloadJobRepository;
import org.uniprot.api.async.download.model.uniprotkb.UniProtKBDownloadJob;
import org.uniprot.api.rest.download.model.JobStatus;

import lombok.extern.slf4j.Slf4j;

@TestConfiguration
@Slf4j
@Profile("offline & integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EmbeddingsTestConsumer {
    @Bean(name = "embeddingsMessageListener")
    public MessageListener embeddingsMessageListener(
            UniProtKBDownloadJobRepository downloadJobRepository) {
        return message -> {
            String jobId = message.getMessageProperties().getHeader("jobId");
            Optional<UniProtKBDownloadJob> optAsyncJob = downloadJobRepository.findById(jobId);
            if (optAsyncJob.isPresent()) {
                log.info("Processing of embedding job " + jobId);
                UniProtKBDownloadJob asyncJob = optAsyncJob.get();
                asyncJob.setStatus(JobStatus.PROCESSING);
                asyncJob.setUpdated(LocalDateTime.now());
                try {
                    Thread.sleep(300); // mimicking while waiting to be picked by aa consumer
                    downloadJobRepository.save(asyncJob);
                    Thread.sleep(100); // mimicking processing of the message
                    asyncJob.setStatus(JobStatus.FINISHED);
                    asyncJob.setUpdated(LocalDateTime.now());
                    asyncJob.setResultFile(jobId);
                    downloadJobRepository.save(asyncJob);
                    log.info("Processing of embeddings job " + jobId + " done.");
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } else {
                Assertions.fail(jobId + " job not found");
            }
        };
    }

    @Bean(name = "embeddingsMessageListenerContainer")
    public MessageListenerContainer embeddingsMessageListenerContainer(
            ConnectionFactory connectionFactory,
            @Qualifier("embeddingsMessageListener") MessageListener embeddingsMessageListener,
            EmbeddingsQueueConfigProperties embeddingsQueueConfigProperties) {
        SimpleMessageListenerContainer simpleMessageListenerContainer =
                new SimpleMessageListenerContainer();
        simpleMessageListenerContainer.setConnectionFactory(connectionFactory);
        simpleMessageListenerContainer.setQueueNames(
                embeddingsQueueConfigProperties.getQueueName());
        simpleMessageListenerContainer.setMessageListener(embeddingsMessageListener);
        simpleMessageListenerContainer.setDefaultRequeueRejected(false);
        simpleMessageListenerContainer.setPrefetchCount(
                embeddingsQueueConfigProperties.getPrefetchCount());
        return simpleMessageListenerContainer;
    }
}
