package org.uniprot.api.async.download.messaging.producer.uniref;

import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.uniprot.api.async.download.messaging.config.uniref.UniRefRabbitTemplate;
import org.uniprot.api.async.download.messaging.producer.common.RabbitProducerMessageService;
import org.uniprot.api.async.download.messaging.repository.DownloadJobRepository;
import org.uniprot.api.async.download.messaging.result.uniref.UniRefAsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.common.DownloadRequest;
import org.uniprot.api.rest.request.HashGenerator;

/**
 * Common for all, UniProtKB, UniParc and UniRef
 *
 * @author sahmad
 * @created 22/11/2022
 */
@Service("uniRef")
@Slf4j
@Profile({"asyncDownload"})
public class UniRefRabbitProducerMessageService extends RabbitProducerMessageService {

    public UniRefRabbitProducerMessageService(
            MessageConverter converter,
            UniRefRabbitTemplate uniRefRabbitTemplate,
            DownloadJobRepository downloadJobRepository,
            HashGenerator<DownloadRequest> uniRefHashGenerator,
            UniRefAsyncDownloadSubmissionRules uniRefAsyncDownloadSubmissionRules,
            UniRefAsyncDownloadFileHandler uniRefAsyncDownloadFileHandler) {
        super(
                converter,
                uniRefRabbitTemplate,
                downloadJobRepository,
                uniRefHashGenerator,
                uniRefAsyncDownloadSubmissionRules,
                uniRefAsyncDownloadFileHandler);
    }
}
